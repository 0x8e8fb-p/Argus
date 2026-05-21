package com.nexusblock.vision

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.view.Display
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Screen capture at 1 fps for visual ad frame classification.
 *
 * Uses MediaProjection with ImageReader at reduced resolution (480p)
 * to minimize CPU and memory impact. Captured frames are fed to the
 * LiteRT classifier to detect ad-specific visual patterns:
 *   - Corner logos ("Skip Ad" countdowns, brand watermarks)
 *   - Black bars with ad text
 *   - Bright uniform backgrounds (common in SSAI transitions)
 *   - Sudden scene changes (cut detection)
 *
 * Performance target: < 5% CPU per frame on Mali-G31.
 */
class MediaProjectionCapture(private val context: Context) {

    companion object {
        private const val TAG = "Argus/Projection"
        private const val CAPTURE_WIDTH = 854   // 480p-ish @ 16:9
        private const val CAPTURE_HEIGHT = 480
        private const val CAPTURE_FPS = 1       // 1 frame per second
        private const val FRAME_INTERVAL_MS = 1000L / CAPTURE_FPS

        fun createPermissionIntent(context: Context): android.content.Intent {
            val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            return mgr.createScreenCaptureIntent()
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var captureJob: Job? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjection: MediaProjection? = null

    private val handler = Handler(Looper.getMainLooper())

    private val _frameData = MutableStateFlow<FrameData?>(null)
    val frameData: StateFlow<FrameData?> = _frameData

    data class FrameData(
        val timestamp: Long,
        val width: Int,
        val height: Int,
        val rgbBytes: ByteArray?
    )

    /**
     * Start capture. Requires a valid MediaProjection obtained via
     * MediaProjectionManager.createScreenCaptureIntent() user consent.
     */
    fun start(projection: MediaProjection) {
        if (captureJob?.isActive == true) return

        mediaProjection = projection

        imageReader = ImageReader.newInstance(
            CAPTURE_WIDTH, CAPTURE_HEIGHT,
            PixelFormat.RGBA_8888, 2
        )

        val metrics = DisplayMetrics()
        val display = context.getSystemService(DisplayManager::class.java)
            ?.getDisplay(Display.DEFAULT_DISPLAY)
        display?.getRealMetrics(metrics)

        val density = metrics.densityDpi

        try {
            virtualDisplay = projection.createVirtualDisplay(
                "ArgusCapture",
                CAPTURE_WIDTH, CAPTURE_HEIGHT, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, handler
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "MediaProjection security exception", e)
            return
        }

        captureJob = scope.launch {
            while (isActive) {
                try {
                    val image = imageReader?.acquireLatestImage()
                    if (image != null) {
                        processImage(image)
                        image.close()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Frame capture error", e)
                }
                delay(FRAME_INTERVAL_MS)
            }
        }

        // Handle MediaProjection stop callback
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.i(TAG, "MediaProjection stopped by system")
                stop()
            }
        }, handler)

        Log.i(TAG, "MediaProjectionCapture started at ${CAPTURE_WIDTH}x${CAPTURE_HEIGHT} @ ${CAPTURE_FPS}fps")
    }

    fun stop() {
        captureJob?.cancel()
        captureJob = null
        try {
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.stop()
        } catch (_: Exception) {}
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
        _frameData.value = null
        Log.i(TAG, "MediaProjectionCapture stopped")
    }

    private fun processImage(image: Image) {
        val planes = image.planes
        if (planes.isEmpty()) return

        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val width = image.width
        val height = image.height

        // Extract RGB bytes efficiently
        val rgbBytes = extractRgbBytes(buffer, width, height, rowStride, pixelStride)

        _frameData.value = FrameData(
            timestamp = System.currentTimeMillis(),
            width = width,
            height = height,
            rgbBytes = rgbBytes
        )
    }

    private fun extractRgbBytes(
        buffer: java.nio.ByteBuffer,
        width: Int, height: Int,
        rowStride: Int, pixelStride: Int
    ): ByteArray {
        val rgbBytes = ByteArray(width * height * 3)
        var offset = 0
        for (row in 0 until height) {
            buffer.position(row * rowStride)
            for (col in 0 until width) {
                val r = buffer.get().toInt() and 0xFF
                val g = buffer.get().toInt() and 0xFF
                val b = buffer.get().toInt() and 0xFF
                buffer.get() // skip alpha
                rgbBytes[offset++] = r.toByte()
                rgbBytes[offset++] = g.toByte()
                rgbBytes[offset++] = b.toByte()
            }
        }
        return rgbBytes
    }


}
