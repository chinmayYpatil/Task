package com.example.task.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.task.data.AppTask
import com.example.task.data.ImageDescriptionTask
import com.example.task.data.PhotoCaptureTask
import com.example.task.data.TextReadingTask
import com.example.task.viewmodel.MainViewModel
import com.example.task.viewmodel.HistoryUiState
import com.example.task.viewmodel.TaskHistoryViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

import coil3.compose.AsyncImage

// FIX: Complete imports for kotlinx-datetime
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Clock
// REMOVED: import kotlin.time.Instant as it conflicts with kotlinx.datetime.Instant

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

// REMOVED: rememberHistoryViewModel factory function as it's now defined in App.kt

@Composable
fun HistoryScreen(viewModel: TaskHistoryViewModel) { // FIX: Accepts ViewModel directly
    val uiState: HistoryUiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Work Report",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp, top = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ReportCard(title = "Total Tasks", value = uiState.totalTasks.toString())
            Spacer(Modifier.width(16.dp))
            ReportCard(title = "Duration Recorded", value = uiState.totalDurationFormatted)
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Tasks",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (uiState.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(top = 32.dp))
        } else if (uiState.errorMessage != null) {
            Text(
                text = "Error: ${uiState.errorMessage}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 32.dp)
            )
        } else if (uiState.tasks.isEmpty()) {
            Text(
                text = "No completed tasks found. Try submitting one!",
                modifier = Modifier.padding(top = 32.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.tasks, key = { it.taskId }) { task ->
                    TaskListItem(
                        task = task,
                        isSelected = uiState.selectedTask?.taskId == task.taskId,
                        onTaskClick = viewModel::selectTask,
                        viewModel = viewModel
                    )
                }
                item {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Button(
            onClick = viewModel::loadTasks,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(vertical = 8.dp),
            enabled = !uiState.isLoading
        ) {
            Text(if (uiState.isLoading) "Loading..." else "Refresh List (Simulate 'See More')")
        }
    }
}

@Composable
private fun ReportCard(title: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun TaskListItem(
    task: AppTask,
    isSelected: Boolean,
    onTaskClick: (AppTask) -> Unit,
    viewModel: TaskHistoryViewModel
) {
    // Corrected to use kotlinx-datetime types now that they are imported
    val formattedTimestamp = remember(task.timestampMs) {
        val instant = Instant.fromEpochMilliseconds(task.timestampMs)
        val date = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        "${date.dayOfMonth.toString().padStart(2, '0')} $month, ${date.year} | ${date.hour.toString().padStart(2, '0')}:${date.minute.toString().padStart(2, '0')}"
    }

    // This property uses .seconds which is an ExperimentalTime API
    val durationText = task.durationSec?.seconds?.let { duration ->
        val minutes = duration.inWholeMinutes
        val seconds = duration.inWholeSeconds % 60
        "${minutes.toString().padStart(2, '0')}m ${seconds.toString().padStart(2, '0')}s"
    } ?: "N/A"


    val preview = when (task) {
        is TextReadingTask -> "Text: ${task.text}"
        is ImageDescriptionTask -> "Image URL: ${task.imageUrl}"
        is PhotoCaptureTask -> {
            if (!task.textDescription.isNullOrBlank()) {
                "Photo Desc: ${task.textDescription}"
            } else if (task.durationSec != null && task.durationSec > 0) {
                "Photo Desc: (Audio Only)"
            } else {
                "Photo: No Description"
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTaskClick(task) },
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${task.taskType.replace(" ", "")}#${task.taskId.takeLast(6)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = task.taskType,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(4.dp))

            Text(
                text = "Duration $durationText | $formattedTimestamp",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = preview,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            AnimatedVisibility(
                visible = isSelected,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    TaskDetailView(task = task, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun TaskDetailView(task: AppTask, viewModel: TaskHistoryViewModel) {
    val uiState: HistoryUiState by viewModel.uiState.collectAsState()

    when (task) {
        is TextReadingTask -> {
            Text(
                text = "Recorded Text:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = task.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        is ImageDescriptionTask -> {
            Text(
                text = "Image Described:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            AsyncImage(
                model = task.imageUrl,
                contentDescription = "Image for Task ${task.taskId}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.LightGray),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(8.dp))
        }
        is PhotoCaptureTask -> {
            Text(
                text = "Captured Photo:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            AsyncImage(
                model = task.imagePath,
                contentDescription = "Captured Photo for Task ${task.taskId}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.LightGray),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(8.dp))
            if (!task.textDescription.isNullOrBlank()) {
                Text(
                    text = "Text Description:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = task.textDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }

    val hasAudio = task.durationSec != null && task.durationSec!! > 0

    if (hasAudio) {
        Text(
            text = "Recorded Audio Playback:",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        AudioPlaybackControl(
            isPlayingAudio = uiState.isPlayingAudio,
            playbackPositionMs = uiState.playbackPositionMs,
            durationSec = task.durationSec!!,
            onPlayClick = viewModel::onPlayClick,
            onSeek = viewModel::onSeek
        )
    } else if (task is PhotoCaptureTask) {
        Text(
            text = "No Audio Description Recorded.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun AudioPlaybackControl(
    isPlayingAudio: Boolean,
    playbackPositionMs: Int,
    durationSec: Int,
    onPlayClick: () -> Unit,
    onSeek: (Float) -> Unit
) {
    val playbackIcon = if (isPlayingAudio) Icons.Filled.Stop else Icons.Filled.PlayArrow
    val contentDescription = if (isPlayingAudio) "Stop Playback" else "Play Recording"
    val durationMs = durationSec * 1000
    val seekProgressFraction = if (durationMs > 0) playbackPositionMs.toFloat() / durationMs else 0f
    val timeText = formatTime(playbackPositionMs, durationSec)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPlayClick) {
            Icon(playbackIcon, contentDescription = contentDescription)
        }

        Slider(
            value = seekProgressFraction,
            onValueChange = { newProgress ->
                onSeek(newProgress)
            },
            enabled = durationMs > 0,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = timeText,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}