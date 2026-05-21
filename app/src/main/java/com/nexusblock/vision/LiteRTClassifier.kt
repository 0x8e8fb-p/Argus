package com.nexusblock.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LiteRT (TensorFlow Lite) on-device image classifier for ad frame detection.
 *
 * Loads a pre-trained quantized MobileNetV3-small model (~2-4 MB) that
 * classifies TV screenshots into "ad" vs "content" categories.
 *
 * Model requirements:
 *   - Input: 224x224 RGB, float32 normalized to [0,1]
 *   - Output: 2-class softmax [content_prob, ad_prob]
 *   - Quantization: INT8 preferred for ~10ms inference on Mali-G31
 *
 * The model is downloaded via OTA update on first launch if not bundled.
 * See assets/models/ for bundled fallback models.
 *
 * Inference time target: < 15ms per frame on Amlogic S905X4.
 */
@Singleton
class LiteRTClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "Argus/LiteRT"
        private const val MODEL_PATH = "models/ad_classifier.tflite"
        private const val LABELS_PATH = "models/labels.txt"
        private const val INPUT_SIZE = 224
        private const val NUM_THREADS = 2
        private const val CONFIDENCE_THRESHOLD = 0.75f
    }

    private var interpreter: Interpreter? = null
    private var labels: List<String> = listOf("content", "ad")
    private val imageProcessor: ImageProcessor
    private val outputBuffer: TensorBuffer

    @Volatile
    var isLoaded = false
        private set

    init {
        // Build image preprocessing pipeline
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(org.tensorflow.lite.support.common.ops.NormalizeOp(0f, 255f))
            .build()

        outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 2), org.tensorflow.lite.DataType.FLOAT32)

        loadModel()
    }

    private fun loadModel() {
        try {
            val model = loadModelFile()
            val options = Interpreter.Options().apply {
                setNumThreads(NUM_THREADS)
                setUseXNNPACK(true)
            }
            interpreter = Interpreter(model, options)

            // Try to load labels from asset
            labels = try {
                context.assets.open(LABELS_PATH).bufferedReader().readLines()
            } catch (e: Exception) {
                listOf("content", "ad")
            }

            isLoaded = true
            Log.i(TAG, "LiteRT model loaded: ${labels.size} labels")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load LiteRT model", e)
            isLoaded = false
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fd = context.assets.openFd(MODEL_PATH)
        FileInputStream(fd.fileDescriptor).use { stream ->
            val channel = stream.channel
            return channel.map(
                FileChannel.MapMode.READ_ONLY,
                fd.startOffset,
                fd.declaredLength
            )
        }
    }

    /**
     * Classify a raw RGB byte array as "ad" or "content".
     *
     * @param rgbBytes Raw RGB bytes (width * height * 3)
     * @param width Frame width
     * @param height Frame height
     * @return ClassificationResult with confidence scores
     */
    fun classify(rgbBytes: ByteArray, width: Int, height: Int): ClassificationResult {
        if (!isLoaded || interpreter == null) {
            return ClassificationResult.unknown()
        }

        return try {
            // Create Bitmap from RGB bytes
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            var offset = 0
            for (i in pixels.indices) {
                val r = rgbBytes[offset++].toInt() and 0xFF
                val g = rgbBytes[offset++].toInt() and 0xFF
                val b = rgbBytes[offset++].toInt() and 0xFF
                pixels[i] = -0x1000000 or (r shl 16) or (g shl 8) or b
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

            // Preprocess
            var tensorImage = TensorImage.fromBitmap(bitmap)
            tensorImage = imageProcessor.process(tensorImage)

            // Run inference
            interpreter?.run(tensorImage.buffer, outputBuffer.buffer.rewind())

            // Parse output
            val scores = outputBuffer.floatArray
            val categories = labels.mapIndexed { index, label ->
                Category(label, scores.getOrElse(index) { 0f })
            }.sortedByDescending { it.score }

            val top = categories.first()
            val isAd = top.label == "ad" && top.score > CONFIDENCE_THRESHOLD

            if (isAd) {
                Log.v(TAG, "Ad detected: ${top.label}=${"%.3f".format(top.score)}")
            }

            ClassificationResult(
                isAd = isAd,
                confidence = top.score,
                allScores = categories.associate { it.label to it.score }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Classification error", e)
            ClassificationResult.unknown()
        }
    }

    /**
     * Classify from a Bitmap directly (for testing).
     */
    fun classifyBitmap(bitmap: Bitmap): ClassificationResult {
        if (!isLoaded || interpreter == null) {
            return ClassificationResult.unknown()
        }

        return try {
            var tensorImage = TensorImage.fromBitmap(bitmap)
            tensorImage = imageProcessor.process(tensorImage)
            interpreter?.run(tensorImage.buffer, outputBuffer.buffer.rewind())

            val scores = outputBuffer.floatArray
            val topLabel = if (scores[1] > scores[0]) "ad" else "content"
            val topScore = scores.maxOrNull() ?: 0f

            ClassificationResult(
                isAd = topLabel == "ad" && topScore > CONFIDENCE_THRESHOLD,
                confidence = topScore,
                allScores = mapOf("content" to scores[0], "ad" to scores[1])
            )
        } catch (e: Exception) {
            Log.w(TAG, "Bitmap classification error", e)
            ClassificationResult.unknown()
        }
    }

    fun close() {
        try {
            interpreter?.close()
        } catch (_: Exception) {}
        interpreter = null
        isLoaded = false
    }

    data class ClassificationResult(
        val isAd: Boolean,
        val confidence: Float,
        val allScores: Map<String, Float>
    ) {
        companion object {
            fun unknown() = ClassificationResult(
                isAd = false,
                confidence = 0f,
                allScores = emptyMap()
            )
        }
    }
}
