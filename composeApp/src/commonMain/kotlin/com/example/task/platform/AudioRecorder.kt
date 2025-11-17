package com.example.task.platform

import kotlinx.coroutines.flow.StateFlow

/**
 * Expected interface for recording and managing audio files.
 */
expect class AudioRecorder {
    // Current state of the recorder
    val isRecording: StateFlow<Boolean>

    // NEW: Current state of the playback
    val isPlaying: StateFlow<Boolean>

    // NEW: Current playback position in milliseconds (Int for Android Media Player)
    val playbackPositionMs: StateFlow<Int>

    // Starts recording to a temporary file.
    fun startRecording(): String? // Returns the path/name of the temporary file

    // Stops recording.
    fun stopRecording(): Int // Returns duration in seconds

    // Starts playback of the last recorded file
    fun playRecording(filePath: String, startPositionMs: Int = 0)

    // Stops playback
    fun stopPlayback()

    // NEW: Seeks to a specific position in the audio (in milliseconds)
    fun seekTo(positionMs: Int)
}

expect fun getAudioRecorder(): AudioRecorder