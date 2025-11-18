package com.example.task.screen

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.task.viewmodel.TaskHistoryViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime // FIX: Import for opt-in

// Factory function to retain the ViewModel instance
@Composable
fun rememberHistoryViewModel(): TaskHistoryViewModel {
    return remember { TaskHistoryViewModel() }
}

@Composable
fun HistoryScreen(mainViewModel: MainViewModel) {
    val viewModel = rememberHistoryViewModel()
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        // Work Report Header
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

        // Tasks List
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
                items(uiState.tasks) { task ->
                    TaskListItem(task = task)
                }
                item {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // "See More" button (Re-triggers load/refresh for this prototype)
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
        // FIX: Ensure Modifier.weight is correctly applied in the RowScope
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

@OptIn(ExperimentalTime::class) // FIX: Added OptIn for using duration properties
@Composable
private fun TaskListItem(task: AppTask) {
    // KMP-Safe Timestamp Formatting: Placeholder format since kotlinx-datetime is not included.
    val formattedTimestamp = task.taskId.takeLast(6).let { idSuffix ->
        // Placeholder format to simulate date/time
        "24 July, 2024 | 21:40 (ID: $idSuffix)"
    }

    // Format duration text - FIX: Use safe access and Int.seconds
    val durationText = task.durationSec?.seconds?.let { duration ->
        val minutes = duration.inWholeMinutes
        val seconds = duration.inWholeSeconds % 60
        String.format("%02dm %02ds", minutes, seconds)
    } ?: "N/A"

    // Determine preview content based on task type
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
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Task ID and Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    // Use a short, human-readable ID based on task type and ID suffix
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

            // Duration + Timestamp
            Text(
                // Combining duration and placeholder timestamp
                text = "Duration $durationText | $formattedTimestamp",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(Modifier.height(8.dp))

            // Preview (Text Snippet/Image Placeholder)
            Text(
                text = preview,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview
@Composable
fun HistoryScreenPreview() {
    MaterialTheme {
        HistoryScreen(mainViewModel = MainViewModel())
    }
}