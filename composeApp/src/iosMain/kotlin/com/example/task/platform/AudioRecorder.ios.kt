package com.example.task.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.time.Clock // NEW IMPORT
import kotlin.time.ExperimentalTime

// Mock duration constants for iOS
private const val MOCK_MIN_DURATION_SEC = 10
private const val MOCK_MAX_DURATION_SEC = 20
private const val MOCK_TOTAL_DURATION_MS = 15000 // 15 seconds mock duration for playback

actual class AudioRecorder {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isRecording = MutableStateFlow(false)
    actual val isRecording: StateFlow<Boolean> = _isRecording

    private val _isPlaying = MutableStateFlow(false)
    actual val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _playbackPositionMs = MutableStateFlow(0)
    actual val playbackPositionMs: StateFlow<Int> = _playbackPositionMs

    private var positionUpdateJob: Job? = null
    private var recordingPath: String? = null

    // Tracks the start time of playback using TimeMark
    private var playbackStartMark: TimeMark? = null

    @OptIn(ExperimentalTime::class)
    actual fun startRecording(): String? {
        if (_isRecording.value) return recordingPath

        println("IOS_AUDIO_CHECK: Simulating microphone permission check. (Actual native implementation required)")

        _isRecording.value = true
        recordingPath = "/mock/ios/audio/${Clock.System.now().toEpochMilliseconds()}.mp3"

        return recordingPath
    }

    actual fun stopRecording(): Int {
        if (!_isRecording.value) return 0

        _isRecording.value = false

        // Mocked: Return a random duration to simulate a valid recording.
        val duration = Random.nextInt(MOCK_MIN_DURATION_SEC, MOCK_MAX_DURATION_SEC + 1)

        // Real implementation would stop and finalize AVAudioRecorder.

        return duration
    }

    actual fun playRecording(filePath: String, startPositionMs: Int) {
        if (_isPlaying.value) {
            stopPlayback()
            return
        }

        // Mocked: Start playback
        _isPlaying.value = true
        _playbackPositionMs.value = startPositionMs

        // Calculate the start mark by subtracting the initial position from 'now'
        playbackStartMark = TimeSource.Monotonic.markNow() - startPositionMs.milliseconds

        startPositionPolling()

        // Real implementation would initialize and start AVAudioPlayer or similar here.
    }

    actual fun stopPlayback() {
        positionUpdateJob?.cancel()
        _isPlaying.value = false
        _playbackPositionMs.value = 0

        // Real implementation would stop and release AVAudioPlayer.
    }

    actual fun seekTo(positionMs: Int) {
        // Mocked: If playing, update the internal flow state immediately.
        if (_isPlaying.value) {
            positionUpdateJob?.cancel()
            _playbackPositionMs.value = positionMs.coerceIn(0, MOCK_TOTAL_DURATION_MS)

            // Reset the TimeMark to reflect the new seek position
            playbackStartMark = TimeSource.Monotonic.markNow() - positionMs.milliseconds

            startPositionPolling()
        }
        // If not playing, the ViewModel handles storing this in `manualSeekPositionMs`.
    }

    private fun startPositionPolling() {
        positionUpdateJob?.cancel()

        val totalDuration = MOCK_TOTAL_DURATION_MS

        positionUpdateJob = scope.launch {
            while (_isPlaying.value && _playbackPositionMs.value < totalDuration) {

                // Calculate elapsed time using TimeMark
                val elapsedTime = playbackStartMark?.elapsedNow()?.inWholeMilliseconds ?: 0L
                val currentPosition = elapsedTime.toInt().coerceAtMost(totalDuration)

                _playbackPositionMs.value = currentPosition

                if (currentPosition >= totalDuration) {
                    stopPlayback() // Auto-stop when mock duration reached
                }
                delay(100L)
            }
        }
    }
}

actual fun getAudioRecorder(): AudioRecorder = AudioRecorder()