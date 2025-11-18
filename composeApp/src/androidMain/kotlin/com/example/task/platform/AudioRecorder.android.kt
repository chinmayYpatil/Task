package com.example.task.platform

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

private const val TAG = "AudioRecorder_Android"

actual class AudioRecorder {
    private val _isRecording = MutableStateFlow(false)
    actual val isRecording: StateFlow<Boolean> = _isRecording

    // NEW: Playback state implementation
    private val _isPlaying = MutableStateFlow(false)
    actual val isPlaying: StateFlow<Boolean> = _isPlaying

    // NEW: Playback position state
    private val _playbackPositionMs = MutableStateFlow(0)
    actual val playbackPositionMs: StateFlow<Int> = _playbackPositionMs

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var positionUpdateJob: Job? = null // Job for position polling

    private var currentRecordingPath: String? = null
    private var recordingStartTime: Long = 0

    @OptIn(ExperimentalTime::class)
    actual fun startRecording(): String? {
        Log.d(TAG, "Starting audio recording.")
        // Ensure not already recording and that the base directory has been set by the Activity
        if (_isRecording.value || Companion.baseDir == null) {
            Log.e(TAG, "Cannot start recording. Already recording or baseDir not initialized.")
            return currentRecordingPath
        }

        // Stop any active playback before recording
        stopPlayback()

        // 1. Setup file path (using a timestamped name in the base directory)
        val tempFile = File(Companion.baseDir, "recording_${System.currentTimeMillis()}.mp3")
        currentRecordingPath = tempFile.absolutePath

        // 2. Initialize and start MediaRecorder
        recorder = try {
            MediaRecorder().apply {
                // Configure recorder settings
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(currentRecordingPath)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)

                // Prepare and start
                prepare()
                start()

                // Update state and time
                recordingStartTime = Clock.System.now().toEpochMilliseconds()
                _isRecording.value = true
                Log.i(TAG, "MediaRecorder started. Path: $currentRecordingPath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaRecorder.", e)
            _isRecording.value = false
            currentRecordingPath = null
            null
        }

        return currentRecordingPath
    }

    @OptIn(ExperimentalTime::class)
    actual fun stopRecording(): Int {
        Log.d(TAG, "Stopping audio recording.")
        if (!_isRecording.value) {
            Log.w(TAG, "Attempted to stop a non-recording state.")
            return 0
        }

        val durationMs: Long = try {
            recorder?.apply {
                stop()
                release()
            }
            // Calculate actual duration
            Clock.System.now().toEpochMilliseconds() - recordingStartTime
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder: ${e.message}")
            0L
        } finally {
            recorder = null
            _isRecording.value = false
        }

        val finalDurationSec = (durationMs.milliseconds.inWholeSeconds).toInt()
        Log.d(TAG, "Recording stopped. Duration: $finalDurationSec seconds.")

        return finalDurationSec
    }

    // UPDATED: Added startPositionMs parameter
    actual fun playRecording(filePath: String, startPositionMs: Int) {
        // If already playing, tapping 'Play' should stop playback (acting as a toggle/stop)
        if (_isPlaying.value) {
            stopPlayback()
            return
        }

        // Stop and release previous player instances before starting a new one
        stopPlayback()

        player = try {
            MediaPlayer().apply {
                setDataSource(filePath)
                setOnPreparedListener {
                    // NEW: Seek to the desired position immediately after prepare()
                    if (startPositionMs > 0) {
                        it.seekTo(startPositionMs)
                    }
                    it.start()
                    _isPlaying.value = true // Update state immediately upon successful start
                    startPositionPolling() // Start updating position
                    Log.i(TAG, "Playback started for: $filePath from $startPositionMs ms")
                }
                setOnCompletionListener {
                    // Release resources and update state when audio finishes naturally
                    stopPositionPolling() // Stop polling
                    it.release()
                    player = null
                    _isPlaying.value = false
                    _playbackPositionMs.value = 0 // Reset position
                    Log.i(TAG, "Playback finished.")
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback for $filePath.", e)
            _isPlaying.value = false
            _playbackPositionMs.value = 0
            null
        }
    }

    actual fun stopPlayback() {
        stopPositionPolling() // Stop polling
        player?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
                release()
                Log.i(TAG, "Playback stopped and released.")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping playback: ${e.message}")
            }
        }
        player = null
        _isPlaying.value = false // Update state upon forceful stop
        _playbackPositionMs.value = 0 // Reset position
    }

    // seekTo logic is fine, but the ViewModel dictates when to use it
    actual fun seekTo(positionMs: Int) {
        player?.apply {
            try {
                // Check if the player is initialized (prepared/started)
                if (isPlaying || currentPosition >= 0) {
                    // Android's MediaPlayer seekTo(int) takes milliseconds
                    seekTo(positionMs)
                    // Update flow manually only if player is not yet playing
                    if (!isPlaying) {
                        _playbackPositionMs.value = positionMs
                    }
                    Log.d(TAG, "Seeked to $positionMs ms.")
                } else {
                    Log.w(TAG, "Cannot seek. Player not ready.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error seeking: ${e.message}")
            }
        }
    }

    private fun startPositionPolling() {
        positionUpdateJob?.cancel()
        // Using Dispatchers.Default for background polling
        positionUpdateJob = CoroutineScope(Dispatchers.Default).launch {
            while (player != null && (player?.isPlaying == true || _playbackPositionMs.value > 0)) { // Continue polling until position is zero
                _playbackPositionMs.value = player?.currentPosition ?: 0
                delay(100L) // Update position every 100ms
            }
            // Ensure final position is 0 if playback finished/stopped
            if (player == null || !player!!.isPlaying) {
                _playbackPositionMs.value = 0
            }
        }
    }

    private fun stopPositionPolling() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    fun onCleared() {
        stopRecording()
        stopPlayback()
    }

    companion object {
        // Must be initialized by Android code (MainActivity) with a valid directory
        var baseDir: File? = null

        fun initialize(cacheDir: File) {
            baseDir = cacheDir
            Log.d(TAG, "AudioRecorder initialized with baseDir: ${baseDir?.absolutePath}")
        }
    }
}

actual fun getAudioRecorder(): AudioRecorder = AudioRecorder()