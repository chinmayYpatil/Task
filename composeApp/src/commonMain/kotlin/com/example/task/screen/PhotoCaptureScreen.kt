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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.task.viewmodel.MainViewModel
import com.example.task.viewmodel.PHOTO_MAX_DURATION_SEC
import com.example.task.viewmodel.PhotoCaptureState
import com.example.task.viewmodel.PhotoCaptureUiState
import com.example.task.viewmodel.PhotoCaptureViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.min

import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

// Reusing helper function from other screens
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
fun PhotoCaptureScreen(viewModel: PhotoCaptureViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Image Capture / Preview Area
        ImageCapturePreviewArea(uiState = uiState, viewModel = viewModel)

        Spacer(Modifier.height(16.dp))

        // 2. Text Description Field
        OutlinedTextField(
            value = uiState.textDescription,
            onValueChange = viewModel::onTextDescriptionChange,
            label = { Text("Describe the photo in your language.") },
            modifier = Modifier.fillMaxWidth().height(80.dp),
            enabled = uiState.state != PhotoCaptureState.PERMISSION_CHECK && uiState.state != PhotoCaptureState.READY_TO_CAPTURE
        )

        Spacer(Modifier.height(16.dp))

        // 3. Audio Recording/Playback Area
        when (uiState.state) {
            PhotoCaptureState.PHOTO_PREVIEW, PhotoCaptureState.RECORDING -> {
                AudioRecordingControl(
                    state = uiState.state,
                    elapsedTime = uiState.elapsedTime,
                    onClick = viewModel::onMicClick
                )
                TextButton(onClick = viewModel::onRetakePhotoClick) {
                    Text("Retake Photo")
                }
            }
            PhotoCaptureState.REVIEW -> {
                AudioReviewControl(
                    uiState = uiState,
                    viewModel = viewModel
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = viewModel::onRetakePhotoClick) {
                        Text("Retake Photo")
                    }
                    if ((uiState.lastRecordedDuration ?: 0) > 0) {
                        TextButton(onClick = viewModel::onRecordAgainClick) {
                            Text("Record Again")
                        }
                    }
                }
            }
            else -> {
                Spacer(Modifier.height(100.dp))
            }
        }

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = viewModel::onSubmitClick,
            enabled = uiState.isSubmitEnabled,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Submit")
        }
    }
}

@Composable
private fun ImageCapturePreviewArea(
    uiState: PhotoCaptureUiState,
    viewModel: PhotoCaptureViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        when (uiState.state) {
            PhotoCaptureState.PERMISSION_CHECK -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Checking Camera Permission...")
                }
            }
            PhotoCaptureState.READY_TO_CAPTURE -> {
                Button(onClick = viewModel::onCaptureImageClick) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Capture Image")
                    Spacer(Modifier.size(8.dp))
                    Text("Capture Image")
                }
            }
            PhotoCaptureState.PHOTO_PREVIEW, PhotoCaptureState.RECORDING, PhotoCaptureState.REVIEW -> {
                uiState.capturedImagePath?.let { path ->
                    AsyncImage(
                        model = path,
                        contentDescription = "Captured Photo Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } ?: run {
                    Text("No Photo Captured Yet.", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun AudioRecordingControl(
    state: PhotoCaptureState,
    elapsedTime: Int,
    onClick: () -> Unit
) {
    val isRecording = state == PhotoCaptureState.RECORDING

    IconButton(
        onClick = onClick,
        enabled = state == PhotoCaptureState.PHOTO_PREVIEW || state == PhotoCaptureState.RECORDING,
        modifier = Modifier
            .size(100.dp)
            .background(if (isRecording) Color.Red else MaterialTheme.colorScheme.primary, CircleShape)
    ) {
        if (isRecording) {
            val progress = min(elapsedTime.toFloat() / PHOTO_MAX_DURATION_SEC, 1f)

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
                    size = Size(radius * 2, radius * 2)
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
        PhotoCaptureState.PHOTO_PREVIEW -> "Tap to Start Audio Description (Optional)"
        PhotoCaptureState.RECORDING -> "${elapsedTime}s / ${PHOTO_MAX_DURATION_SEC}s (Tap to stop)"
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
private fun AudioReviewControl(
    uiState: PhotoCaptureUiState,
    viewModel: PhotoCaptureViewModel
) {
    if ((uiState.lastRecordedDuration ?: 0) > 0) {
        val playbackIcon = if (uiState.isPlayingAudio) Icons.Filled.Stop else Icons.Filled.PlayArrow
        val contentDescription = if (uiState.isPlayingAudio) "Stop Playback" else "Play Recording"

        val durationSec = uiState.lastRecordedDuration ?: 0
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

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            CheckRow(
                text = "No background noise",
                checked = uiState.checkboxState.noNoise,
                onCheckedChange = { viewModel.onCheckboxToggled(0, it) }
            )
            CheckRow(
                text = "No mistakes while describing",
                checked = uiState.checkboxState.noMistakes,
                onCheckedChange = { viewModel.onCheckboxToggled(1, it) }
            )
            CheckRow(
                text = "Beech me koi galti nahi hai",
                checked = uiState.checkboxState.hindiCheck,
                onCheckedChange = { viewModel.onCheckboxToggled(2, it) }
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text("Mandatory Checks:")
            CheckRow(
                text = "No background noise",
                checked = uiState.checkboxState.noNoise,
                onCheckedChange = { viewModel.onCheckboxToggled(0, it) }
            )
            CheckRow(
                text = "No mistakes",
                checked = uiState.checkboxState.noMistakes,
                onCheckedChange = { viewModel.onCheckboxToggled(1, it) }
            )
            CheckRow(
                text = "Beech me koi galti nahi hai",
                checked = uiState.checkboxState.hindiCheck,
                onCheckedChange = { viewModel.onCheckboxToggled(2, it) }
            )
        }
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
