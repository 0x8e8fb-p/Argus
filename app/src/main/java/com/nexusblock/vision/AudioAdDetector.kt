package com.nexusblock.vision

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Audio loudness monitor for detecting SSAI ad breaks.
 *
 * TV advertisements are typically 6-12 dB louder than content (CALM Act
 * violations are rampant on streaming platforms). This detector samples
 * the audio output stream, computes RMS loudness in LUFS-like scale,
 * and fires a signal when a sustained loudness spike is detected.
 *
 * Architecture:
 *   AudioRecord (STREAM_MUSIC) → RMS buffer → moving average →
 *   threshold comparator → StateFlow<AudioAdState>
 *
 * Performance: ~2% CPU on Amlogic S905X4. Runs on Dispatchers.Default
 * with a small circular buffer.
 */
class AudioAdDetector(context: Context) {

    companion object {
        private const val TAG = "Argus/AudioDetector"

        // Audio params
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 2

        // Detection params
        private const val WINDOW_MS = 500L           // Analysis window
        private const val SPIKE_THRESHOLD_DB = 6.0   // dB above baseline
        private const val SUSTAIN_MS = 3000L         // Must stay loud for 3s
        private const val BASELINE_CALIBRATION_MS = 5000L // Calibrate content baseline
    }

    private val minBufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
    )
    private val bufferSize = minBufferSize * BUFFER_SIZE_MULTIPLIER

    private var audioRecord: AudioRecord? = null
    private var detectorJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<AudioAdState>(AudioAdState.Content)
    val state: StateFlow<AudioAdState> = _state

    // Internal analysis state
    private var baselineDb = -60.0     // Content baseline loudness
    private var spikeStartTime = 0L
    private var isCalibrating = true
    private var calibrationStartTime = 0L
    private val calibrationReadings = mutableListOf<Double>()

    sealed class AudioAdState {
        object Content : AudioAdState()
        data class PossibleAd(val loudnessDb: Double) : AudioAdState()
        data class ConfirmedAd(val loudnessDb: Double, val durationMs: Long) : AudioAdState()
    }

    fun start() {
        if (detectorJob?.isActive == true) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.REMOTE_SUBMIX,
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize
            )

            // Fallback if REMOTE_SUBMIX not available (requires system app or root)
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT,
                    SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.w(TAG, "AudioRecord initialization failed")
                return
            }

            audioRecord?.startRecording()
            isCalibrating = true
            calibrationStartTime = System.currentTimeMillis()
            calibrationReadings.clear()

            detectorJob = scope.launch {
                val readBuffer = ShortArray(minBufferSize)
                while (isActive) {
                    val read = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: 0
                    if (read > 0) {
                        processAudioBuffer(readBuffer, read)
                    }
                    delay(WINDOW_MS)
                }
            }
            Log.i(TAG, "AudioAdDetector started")
        } catch (e: SecurityException) {
            Log.w(TAG, "RECORD_AUDIO permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio detector", e)
        }
    }

    fun stop() {
        detectorJob?.cancel()
        detectorJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        _state.value = AudioAdState.Content
        Log.i(TAG, "AudioAdDetector stopped")
    }

    private fun processAudioBuffer(buffer: ShortArray, readCount: Int) {
        // Compute RMS loudness
        var sum = 0.0
        for (i in 0 until readCount) {
            sum += buffer[i].toDouble() * buffer[i].toDouble()
        }
        val rms = sqrt(sum / readCount)
        val db = if (rms > 0) 20 * log10(rms) else -100.0

        val now = System.currentTimeMillis()

        if (isCalibrating) {
            calibrationReadings.add(db)
            if (now - calibrationStartTime >= BASELINE_CALIBRATION_MS) {
                // Compute baseline as median of calibration readings
                baselineDb = calibrationReadings.sorted()
                    .let { it[it.size / 2] }
                isCalibrating = false
                Log.i(TAG, "Audio baseline calibrated: %.1f dB".format(baselineDb))
            }
            return
        }

        // Adaptive baseline: slowly track content loudness when not in ad
        if (_state.value is AudioAdState.Content) {
            baselineDb = baselineDb * 0.95 + db * 0.05
        }

        val deltaDb = db - baselineDb

        when (_state.value) {
            is AudioAdState.Content -> {
                if (deltaDb > SPIKE_THRESHOLD_DB) {
                    spikeStartTime = now
                    _state.value = AudioAdState.PossibleAd(db)
                    Log.v(TAG, "Possible ad detected: %.1f dB (delta %.1f)".format(db, deltaDb))
                }
            }
            is AudioAdState.PossibleAd -> {
                if (deltaDb > SPIKE_THRESHOLD_DB) {
                    val duration = now - spikeStartTime
                    if (duration >= SUSTAIN_MS) {
                        _state.value = AudioAdState.ConfirmedAd(db, duration)
                        Log.i(TAG, "Confirmed SSAI ad: %.1f dB for %d ms".format(db, duration))
                    } else {
                        _state.value = AudioAdState.PossibleAd(db)
                    }
                } else {
                    _state.value = AudioAdState.Content
                    Log.v(TAG, "Ad loudness subsided, back to content")
                }
            }
            is AudioAdState.ConfirmedAd -> {
                if (deltaDb > SPIKE_THRESHOLD_DB) {
                    val duration = now - spikeStartTime
                    _state.value = AudioAdState.ConfirmedAd(db, duration)
                } else {
                    _state.value = AudioAdState.Content
                    Log.i(TAG, "SSAI ad ended, restoring content")
                }
            }
        }
    }
}
