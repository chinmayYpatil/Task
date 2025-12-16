package com.example.task.platform

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

actual class DecibelMeter {
    private val _currentDb = MutableStateFlow(0f)
    actual val currentDb: StateFlow<Float> = _currentDb

    private var mockJob: kotlinx.coroutines.Job? = null

    actual fun start() {
        // Mocked: Simulate rising and falling noise
        mockJob?.cancel()
        mockJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            while (true) {
                // Mock value between 0 and 45 dB
                _currentDb.value = (0..45).random().toFloat()
                delay(timeMillis = 200L)
            }
        }
    }

    actual fun stop() {
        mockJob?.cancel()
        _currentDb.value = 0f
    }
}

actual fun getDecibelMeter(): DecibelMeter = DecibelMeter()