package com.example.task.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider // NEW IMPORT
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.task.viewmodel.MainViewModel
import com.example.task.viewmodel.MAX_DURATION_SEC
import com.example.task.viewmodel.TextRecordingState // UPDATED: Renamed enum import
import com.example.task.viewmodel.TextReadingUiState
import com.example.task.viewmodel.TextReadingViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.min

// REMOVED: rememberTextReadingViewModel factory function as it's now defined in App.kt

// Helper function to format milliseconds to current / total (mm:ss / mm:ss)
private fun formatTime(milliseconds: Int, totalDurationSec: Int): String {
    val totalSeconds = totalDurationSec.toLong()
    val currentSeconds = (milliseconds / 1000).toLong()

    val currentMin = currentSeconds / 60
    val currentSec = currentSeconds % 60

    val totalMin = totalSeconds / 60
    val totalSec = totalSeconds % 60

    val current = "${currentMin}:${currentSec.toString().padStart(2, '0')}"
    val total = "${totalMin}:${totalSec.toString().padStart(2, '0')}"

    return "$current / $total"
}


@Composable
fun TextReadingScreen(viewModel: TextReadingViewModel) { // FIX: Accepts ViewModel directly
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Text Passage
        Text(
            text = "Passage to read:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.LightGray.copy(alpha = 0.2f))
                .padding(16.dp)
        ) {
            // Display dynamic text coloring
            PassageTextDisplay(uiState = uiState)

            if (uiState.recordingState == TextRecordingState.LOADING_TEXT) { // UPDATED
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }

        Spacer(Modifier.height(16.dp))

        // 2. Instructions
        Text(
            text = "Read the passage aloud in your native language.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 3. Mic Button / Recording Indicator
        when (uiState.recordingState) {
            TextRecordingState.READY_TO_RECORD, TextRecordingState.RECORDING -> { // UPDATED
                RecordButton(
                    state = uiState.recordingState,
                    elapsedTime = uiState.elapsedTime,
                    onClick = viewModel::onMicClick
                )
            }
            TextRecordingState.REVIEW -> { // UPDATED
                // Recording Results Area
                RecordingResultArea(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
            else -> { /* Loading text state, typically shows a spacer/loader */
                Spacer(Modifier.size(100.dp).padding(top = 8.dp))
            }
        }

        Spacer(Modifier.height(32.dp))

        // 4. Error Message (Visible in any state)
        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(16.dp))

        // 5. Submit Button (Visible only in Review state)
        if (uiState.recordingState == TextRecordingState.REVIEW) { // UPDATED
            Button(
                onClick = viewModel::onSubmitClick,
                enabled = uiState.isSubmitEnabled,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Submit")
            }
        }
    }
}

/**
 * Renders the passage text without any word highlighting.
 */
@Composable
private fun PassageTextDisplay(uiState: TextReadingUiState) {
    if (uiState.passageWords.isEmpty()) {
        Text(
            text = uiState.passageText,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black
        )
        return
    }

    val annotatedString = buildAnnotatedString {
        uiState.passageWords.forEachIndexed { index, word ->
            // REMOVED HIGHLIGHTING: Render all words as standard black text.
            withStyle(
                style = SpanStyle(
                    color = Color.Black,
                    fontWeight = FontWeight.Normal
                )
            ) {
                // Append the word followed by a space
                append(word)
                append(" ")
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge
    )
}

/**
 * Single composable for Start/Stop Mic Button and Timer Indicator.
 */
@Composable
private fun RecordButton(
    state: TextRecordingState, // UPDATED
    elapsedTime: Int,
    onClick: () -> Unit
) {
    val isRecording = state == TextRecordingState.RECORDING // UPDATED

    IconButton(
        onClick = onClick,
        // Enabled when ready to start or when recording to stop
        enabled = state == TextRecordingState.READY_TO_RECORD || state == TextRecordingState.RECORDING, // UPDATED
        modifier = Modifier
            .size(100.dp)
            .background(if (isRecording) Color.Red else MaterialTheme.colorScheme.primary, CircleShape)
    ) {
        if (isRecording) {
            // Timer Indicator when recording
            val progress = min(elapsedTime.toFloat() / MAX_DURATION_SEC, 1f)

            // Draw progress circle
            Canvas(modifier = Modifier.size(90.dp)) {
                val strokeWidth = 5.dp.toPx()
                val radius = size.minDimension / 2

                // Outer circle indicating max duration (20s)
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )

                // Progress arc
                drawArc(
                    color = Color.White,
                    startAngle = 270f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                    topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
            }

            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Stop Recording",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        } else {
            // Default Mic Icon when ready
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Start Recording",
                tint = contentColorFor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(48.dp)
            )
        }
    }

    val statusText = when (state) {
        TextRecordingState.READY_TO_RECORD -> "Tap to Start Recording" // UPDATED
        TextRecordingState.RECORDING -> "${elapsedTime}s / ${MAX_DURATION_SEC}s (Tap to stop)" // UPDATED
        else -> ""
    }

    if (statusText.isNotEmpty()) {
        Text(
            text = statusText,
            color = if (isRecording) Color.Red else Color.Black,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun RecordingResultArea(
    uiState: TextReadingUiState,
    viewModel: TextReadingViewModel
) {
    // Determine which icon to show: Play or Stop
    val playbackIcon = if (uiState.isPlayingAudio) Icons.Filled.Stop else Icons.Filled.PlayArrow
    val contentDescription = if (uiState.isPlayingAudio) "Stop Playback" else "Play Recording"

    val durationSec = uiState.lastRecordedDuration ?: uiState.elapsedTime
    val durationMs = durationSec * 1000

    // 1. Calculate the actual flow progress (used as the base value)
    val flowProgress = if (durationMs > 0) {
        uiState.playbackPositionMs.toFloat() / durationMs
    } else {
        0f
    }

    // 2. Determine the progress based on manual seek position when not playing
    val initialSeekProgress = if (durationMs > 0) {
        uiState.manualSeekPositionMs.toFloat() / durationMs
    } else {
        0f
    }

    // Local state to track the slider position while dragging
    var seekPositionFraction by remember { mutableStateOf(initialSeekProgress) }
    var isSeeking by remember { mutableStateOf(false) }

    // If the audio starts playing or finishes, reset the local state to track the flow progress.
    // When not seeking, the slider position tracks either the live position (if playing) or the stored manual position.
    if (!isSeeking) {
        // Use live progress if playing, otherwise use the stored manual seek position
        seekPositionFraction = if (uiState.isPlayingAudio) flowProgress else initialSeekProgress
    }

    // Determine which millisecond value to display: local drag position, live position, or stored manual position
    val displayPositionMs = if (isSeeking)
        (seekPositionFraction * durationMs).toInt()
    else if (uiState.isPlayingAudio)
        uiState.playbackPositionMs
    else
        uiState.manualSeekPositionMs

    val timeText = formatTime(displayPositionMs, durationSec)


    // 1. Playback Bar
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Playback button calls the toggle function
        IconButton(onClick = viewModel::onPlayClick) {
            Icon(playbackIcon, contentDescription = contentDescription)
        }

        Slider(
            value = seekPositionFraction,
            onValueChange = { newProgress ->
                // 1. Set seeking state and update local position for visual dragging
                isSeeking = true
                seekPositionFraction = newProgress
            },
            onValueChangeFinished = {
                // 2. Commit the seek operation when the user lifts their finger
                // This will either seek the player (if playing) or update the manualSeekPositionMs (if stopped)
                viewModel.onSeek(seekPositionFraction)
                isSeeking = false
            },
            enabled = durationMs > 0, // Enable only if there's audio to play
            modifier = Modifier.weight(1f)
        )

        // Display dynamic time
        Text(
            text = timeText,
            modifier = Modifier.padding(start = 8.dp)
        )
    }

    // 2. Checkboxes
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        // Checkbox 1: No background noise
        CheckRow(
            text = "No background noise",
            checked = uiState.checkboxState.noNoise,
            onCheckedChange = { viewModel.onCheckboxToggled(0, it) }
        )
        // Checkbox 2: No mistakes while reading
        CheckRow(
            text = "No mistakes while reading",
            checked = uiState.checkboxState.noMistakes,
            onCheckedChange = { viewModel.onCheckboxToggled(1, it) }
        )
        // Checkbox 3: Beech me koi galti nahi hai
        CheckRow(
            text = "Beech me koi galti nahi hai",
            checked = uiState.checkboxState.hindiCheck,
            onCheckedChange = { viewModel.onCheckboxToggled(2, it) }
        )
    }

    // 3. Record Again Button
    Button(
        onClick = viewModel::onRecordAgainClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = Color.Gray
        )
    ) {
        Text("Record again")
    }
}

@Composable
private fun CheckRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text)
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}