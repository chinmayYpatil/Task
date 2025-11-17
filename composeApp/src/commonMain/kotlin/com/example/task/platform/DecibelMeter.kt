package com.example.task.platform

import kotlinx.coroutines.flow.StateFlow

/**
 * Expected interface for starting and stopping microphone input
 * and providing live decibel readings.
 */
expect class DecibelMeter {
    // Current live decibel reading (0.0 to 60.0, or -1.0 if not running)
    val currentDb: StateFlow<Float>

    // Starts the mic input. Requires RECORD_AUDIO permission.
    fun start()

    // Stops the mic input and clears resources.
    fun stop()
}

// Utility to get the platform-specific DecibelMeter implementation
expect fun getDecibelMeter(): DecibelMeter