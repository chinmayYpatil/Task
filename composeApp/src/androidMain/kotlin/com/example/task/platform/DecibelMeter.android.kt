package com.example.task.platform

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.log10

private const val TAG = "NoiseTestApp_Meter"

// Audio recording configuration
private const val SAMPLE_RATE = 44100
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

// The maximum amplitude value for a 16-bit PCM signal is 32767
private const val MAX_PCM_AMPLITUDE = 32767.0
// Max dB value used for scaling (based on the gauge UI max)
private const val DB_MAX_DISPLAY = 60f

actual class DecibelMeter {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var audioRecord: AudioRecord? = null
    private val _currentDb = MutableStateFlow(0f)
    actual val currentDb: StateFlow<Float> = _currentDb
    private var job: Job? = null

    actual fun start() {
        Log.d(TAG, "Attempting to start DecibelMeter. Current job active: ${job?.isActive}")
        if (job?.isActive == true) return

        // 1. Initialize AudioRecord (wrapped in try-catch for SecurityException)
        try {
            audioRecord = AudioRecord(
                // FIX: Switching to UNPROCESSED source. This is another option for raw audio that might
                // not be as aggressively amplified as VOICE_RECOGNITION.
                MediaRecorder.AudioSource.UNPROCESSED,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE * 2
            )
        } catch (e: SecurityException) {
            // Explicitly handle SecurityException on constructor call
            Log.e(TAG, "FATAL: SecurityException on AudioRecord constructor. Permission denied.", e)
            _currentDb.value = -1f
            return
        }

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialization FAILED. State: ${audioRecord?.state}")
            _currentDb.value = 0f
            audioRecord = null
            return
        }
        Log.d(TAG, "AudioRecord initialized successfully. Buffer size: ${BUFFER_SIZE * 2}")

        try {
            audioRecord?.startRecording()
            Log.i(TAG, "AudioRecord started recording. State: ${audioRecord?.recordingState}")
            startReading()
        } catch (e: Exception) {
            // Catches any exception on startRecording() (e.g., IllegalStateException)
            Log.e(TAG, "Exception during startRecording().", e)
            _currentDb.value = -1f
            stop()
        }
    }

    private fun startReading() {
        val buffer = ShortArray(BUFFER_SIZE)

        job = scope.launch {
            while (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING && audioRecord != null) {
                // Read audio data into the buffer
                val read = audioRecord!!.read(buffer, 0, BUFFER_SIZE)

                if (read > 0) {
                    var maxAmplitude: Short = 0
                    for (i in 0 until read) {
                        val absValue = kotlin.math.abs(buffer[i].toInt())
                        if (absValue > maxAmplitude) {
                            maxAmplitude = absValue.toShort()
                        }
                    }

                    if (maxAmplitude > 0) {
                        // Calculate dB value relative to MAX_PCM_AMPLITUDE
                        val amplitudeRatio = maxAmplitude.toDouble() / MAX_PCM_AMPLITUDE
                        var db = 20.0 * log10(amplitudeRatio)

                        // Scale the relative dB value (-90 to 0) to the gauge's 0 to 60 range.
                        val scaledDb = (DB_MAX_DISPLAY + db).toFloat().coerceIn(0f, DB_MAX_DISPLAY)
                        _currentDb.value = scaledDb
                        // Use String.format as it is available in the Android context
                        Log.v(TAG, "DB Read: ${String.format("%.1f", scaledDb)} dB (Raw Max Amp: $maxAmplitude)")
                    } else {
                        _currentDb.value = 0f
                        Log.v(TAG, "DB Read: 0.0 dB (Raw Max Amp: $maxAmplitude)")
                    }
                } else {
                    Log.w(TAG, "AudioRecord.read() returned $read")
                }
                delay(50L)
            }
            Log.d(TAG, "Reading loop finished.")
        }
    }

    actual fun stop() {
        Log.d(TAG, "Stopping DecibelMeter.")
        job?.cancel()
        audioRecord?.apply {
            try {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
                Log.i(TAG, "AudioRecord stopped and released.")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping/releasing AudioRecord: ${e.message}")
            }
        }
        audioRecord = null
        _currentDb.value = 0f
    }

    companion object {
        fun initialize(cacheDir: java.io.File) {
            Log.d(TAG, "DecibelMeter initialize called (No-op with AudioRecord).")
        }
    }
}

actual fun getDecibelMeter(): DecibelMeter = DecibelMeter()