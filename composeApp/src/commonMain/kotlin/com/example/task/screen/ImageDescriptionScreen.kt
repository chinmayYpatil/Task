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
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.task.viewmodel.ImageDescriptionUiState
import com.example.task.viewmodel.ImageDescriptionViewModel
import com.example.task.viewmodel.MainViewModel
import com.example.task.viewmodel.IMAGE_MAX_DURATION_SEC
import com.example.task.viewmodel.ImageRecordingState
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.min

// NEW IMPORTS FOR IMAGE DISPLAY (Coil)
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Size // Explicit import to resolve compilation conflicts

// Mock logger
private fun logDebug(tag: String, message: String) {
    println("DEBUG | $tag: $message")
}
private const val TAG_UI = "ImageDescApp_UI" // Log Tag for the Screen

// Factory function to retain the ViewModel instance
@Composable
fun rememberImageDescriptionViewModel(mainViewModel: MainViewModel): ImageDescriptionViewModel {
    return remember { ImageDescriptionViewModel(mainViewModel = mainViewModel) }
}

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
fun ImageDescriptionScreen(mainViewModel: MainViewModel) {
    val viewModel = rememberImageDescriptionViewModel(mainViewModel = mainViewModel)
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Image/Display Area
        Text(
            text = "Image to describe:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.DarkGray.copy(alpha = 0.5f))
                .padding(1.dp) // Minor padding for border effect
        ) {
            // FIX: Added non-null assertion (!!) to resolve smart cast error when passing nullable state property
            if (uiState.imageUrl != null && uiState.recordingState != ImageRecordingState.LOADING_TASK) {
                NetworkImageDisplay(url = uiState.imageUrl!!)
            }

            if (uiState.recordingState == ImageRecordingState.LOADING_TASK) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }

        Spacer(Modifier.height(16.dp))

        // 2. Instructions
        Text(
            text = uiState.instruction,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 3. Mic Button / Recording Indicator
        when (uiState.recordingState) {
            ImageRecordingState.READY_TO_RECORD, ImageRecordingState.RECORDING -> {
                ImageDescriptionRecordButton(
                    state = uiState.recordingState,
                    elapsedTime = uiState.elapsedTime,
                    onClick = viewModel::onMicClick
                )
            }
            ImageRecordingState.REVIEW -> {
                // Recording Results Area
                ImageDescriptionRecordingResultArea(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
            else -> {
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
        if (uiState.recordingState == ImageRecordingState.REVIEW) {
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
 * REPLACEMENT for ImagePlaceholder: A composable that visually represents a loaded image.
 * This component is responsible for displaying the network image based on the URL.
 */
@Composable
private fun NetworkImageDisplay(url: String) {
    logDebug(TAG_UI, "NetworkImageDisplay rendering for URL: $url")

    // FIX: Use Coil's AsyncImage and REMOVE the problematic 'error' composable parameter
    // to resolve the compilation errors (Argument type mismatch and @Composable context).
    AsyncImage(
        model = url,
        contentDescription = "Image to describe",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit, // Use Fit to ensure the whole image fits in the allocated box
    )
}


@Composable
private fun ImageDescriptionRecordButton(
    state: ImageRecordingState,
    elapsedTime: Int,
    onClick: () -> Unit
) {
    val isRecording = state == ImageRecordingState.RECORDING

    IconButton(
        onClick = onClick,
        enabled = state == ImageRecordingState.READY_TO_RECORD || state == ImageRecordingState.RECORDING,
        modifier = Modifier
            .size(100.dp)
            .background(if (isRecording) Color.Red else MaterialTheme.colorScheme.primary, CircleShape)
    ) {
        if (isRecording) {
            val progress = min(elapsedTime.toFloat() / IMAGE_MAX_DURATION_SEC, 1f)

            Canvas(modifier = Modifier.size(90.dp)) {
                val strokeWidth = 5.dp.toPx()
                val radius = size.minDimension / 2

                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )

                drawArc(
                    color = Color.White,
                    startAngle = 270f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                    topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2),
                    size = Size(radius * 2, radius * 2) // Using the explicit import Size
                )
            }

            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Stop Recording",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Start Recording",
                tint = contentColorFor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(48.dp)
            )
        }
    }

    val statusText = when (state) {
        ImageRecordingState.READY_TO_RECORD -> "Tap to Start Recording"
        ImageRecordingState.RECORDING -> "${elapsedTime}s / ${IMAGE_MAX_DURATION_SEC}s (Tap to stop)"
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
private fun ImageDescriptionRecordingResultArea(
    uiState: ImageDescriptionUiState,
    viewModel: ImageDescriptionViewModel
) {
    val playbackIcon = if (uiState.isPlayingAudio) Icons.Filled.Stop else Icons.Filled.PlayArrow
    val contentDescription = if (uiState.isPlayingAudio) "Stop Playback" else "Play Recording"

    val durationSec = uiState.lastRecordedDuration ?: uiState.elapsedTime
    val durationMs = durationSec * 1000

    val flowProgress = if (durationMs > 0) {
        uiState.playbackPositionMs.toFloat() / durationMs
    } else {
        0f
    }

    val initialSeekProgress = if (durationMs > 0) {
        uiState.manualSeekPositionMs.toFloat() / durationMs
    } else {
        0f
    }

    var seekPositionFraction by remember { mutableStateOf(initialSeekProgress) }
    var isSeeking by remember { mutableStateOf(false) }

    if (!isSeeking) {
        seekPositionFraction = if (uiState.isPlayingAudio) flowProgress else initialSeekProgress
    }

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
        IconButton(onClick = viewModel::onPlayClick) {
            Icon(playbackIcon, contentDescription = contentDescription)
        }

        Slider(
            value = seekPositionFraction,
            onValueChange = { newProgress ->
                isSeeking = true
                seekPositionFraction = newProgress
            },
            onValueChangeFinished = {
                viewModel.onSeek(seekPositionFraction)
                isSeeking = false
            },
            enabled = durationMs > 0,
            modifier = Modifier.weight(1f)
        )

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
        // Checkbox 2: No mistakes while describing
        CheckRow(
            text = "No mistakes while describing",
            checked = uiState.checkboxState.noMistakes,
            onCheckedChange = { viewModel.onCheckboxToggled(1, it) }
        )
        // Checkbox 3: Hindi Check (re-used)
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


@Preview
@Composable
fun ImageDescriptionScreenPreview() {
    MaterialTheme {
        ImageDescriptionScreen(mainViewModel = MainViewModel())
    }
}