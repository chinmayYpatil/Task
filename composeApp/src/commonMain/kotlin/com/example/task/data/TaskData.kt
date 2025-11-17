package com.example.task.data

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Model representing a completed Text Reading Task submission.
 */
data class TextReadingTask @OptIn(ExperimentalTime::class) constructor(
    val taskType: String = "text_reading",
    val text: String,
    val audioPath: String,
    val durationSec: Int,
    val timestamp: Instant // Using Instant for timestamp
)