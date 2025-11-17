package com.example.task.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.task.navigation.Screen
import com.example.task.platform.DecibelMeter
import com.example.task.platform.getDecibelMeter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Logs needs to be defined safely for KMP: we use a helper function or assume android's Log is used if on Android.
// In a real KMP project, you'd use a common logger, but here we'll assume Android Log for debugging.
private fun logDebug(tag: String, message: String) {
    // This is a placeholder for a cross-platform log function.
    // When running on Android, this will still output to the logcat.
    // On JVM/Desktop/Browser/iOS it will use standard output.
    println("DEBUG | $tag: $message")
}
private const val TAG = "NoiseTestApp_VM"

// Threshold for noise test (40 dB)
private const val NOISE_THRESHOLD_DB = 40f
private const val TEST_DURATION_SECONDS = 5
private const val RESULT_DISPLAY_DELAY_MS = 2000L

enum class TestState {
    IDLE,       // Ready to start
    RUNNING,    // Recording live data
    ANALYZING,  // Test finished, calculating result
    COMPLETED   // Result displayed
}

data class NoiseTestUiState(
    val state: TestState = TestState.IDLE,
    val liveDb: Float = 0f,
    val avgDb: Float? = null,
    val message: String = "Before you can start the call we will have to check your ambient noise level.",
    val isTestSuccessful: Boolean? = null
)

class NoiseTestViewModel(
    private val mainViewModel: MainViewModel,
    private val meter: DecibelMeter = getDecibelMeter()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoiseTestUiState())
    val uiState: StateFlow<NoiseTestUiState> = _uiState.asStateFlow()

    private val recordedDbs = mutableListOf<Float>()
    private var dbCollectionJob: Job? = null

    init {
        // Stop meter when ViewModel is cleared
        viewModelScope.launch {
            meter.currentDb.collectLatest { newDb ->
                // FIX: Use manual formatting logic instead of String.format()
                val formattedDb = ((newDb * 10).roundToInt() / 10f)
                logDebug(TAG, "Meter Db Update Received: $formattedDb")
                if (newDb > 0 && _uiState.value.state == TestState.RUNNING) {
                    recordedDbs.add(newDb)
                }
                _uiState.update { state -> state.copy(liveDb = newDb) }
            }
        }
    }

    fun startTest() {
        if (_uiState.value.state != TestState.IDLE && _uiState.value.isTestSuccessful != false) {
            logDebug(TAG, "Test already running or analyzing. Current state: ${_uiState.value.state}")
            return
        }

        logDebug(TAG, "Starting Noise Test. Duration: $TEST_DURATION_SECONDS seconds.")

        // 1. Reset and start recording
        recordedDbs.clear()
        _uiState.update { it.copy(
            state = TestState.RUNNING,
            message = "Recording ambient noise for $TEST_DURATION_SECONDS seconds...",
            isTestSuccessful = null
        ) }
        meter.start()

        // 2. Set duration and analyze
        viewModelScope.launch {
            delay(TEST_DURATION_SECONDS * 1000L)
            stopAndAnalyze()
        }
    }

    private fun stopAndAnalyze() {
        meter.stop()
        _uiState.update { it.copy(state = TestState.ANALYZING, message = "Analyzing result...") }
        logDebug(TAG, "Test finished. Analyzing ${recordedDbs.size} samples.")


        viewModelScope.launch {
            val averageDb = recordedDbs.filter { it >= 0 }.takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 0f
            val isSuccess = averageDb < NOISE_THRESHOLD_DB // Success if < 40 dB

            // FIX: Use explicit formatting function
            val formattedAvgDb = ((averageDb * 10).roundToInt() / 10f).toString()

            logDebug(TAG, "Average Db: $formattedAvgDb, Target: $NOISE_THRESHOLD_DB. Success: $isSuccess")


            val resultMessage = if (isSuccess) {
                "Good to proceed (Avg: $formattedAvgDb dB)"
            } else {
                "Please move to a quieter place (Avg: $formattedAvgDb dB)"
            }

            _uiState.update {
                it.copy(
                    state = TestState.COMPLETED,
                    avgDb = averageDb,
                    message = resultMessage,
                    isTestSuccessful = isSuccess
                )
            }

            // 3. Handle result display and retry/manual navigation
            if (!isSuccess) {
                // REQUIRED: If failed (>= 40 dB), reset to IDLE after a delay to allow for retry
                delay(RESULT_DISPLAY_DELAY_MS)
                _uiState.update { NoiseTestUiState() }
            }
            // REQUIRED: If successful (< 40 dB), stay in COMPLETED state and wait for user click.
        }
    }

    /**
     * Called when the user clicks the "Good to proceed" button.
     */
    fun navigateToTaskSelection() {
        mainViewModel.navigateTo(Screen.TaskSelection)
    }

    override fun onCleared() {
        super.onCleared()
        logDebug(TAG, "ViewModel cleared. Stopping meter.")
        meter.stop()
    }
}