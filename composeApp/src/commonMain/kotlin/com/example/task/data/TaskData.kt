package com.example.task.data

import kotlin.time.ExperimentalTime
import kotlin.time.Instant // Keep import but change usage
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.serialization.Contextual // NEW IMPORT

// Utility to generate a pseudo-unique ID for KMP based on timestamp
@OptIn(ExperimentalTime::class)
private fun generateTaskId(): String = Clock.System.now().toEpochMilliseconds().toString()

// Define a common sealed interface for all tasks
@Serializable // Must be serializable for polymorphism to work
sealed interface AppTask {
    val taskId: String
    val taskType: String
    // FIX: Change Instant to Long (Epoch Milliseconds)
    val timestampMs: Long
    val durationSec: Int?
}


@Serializable
data class Product(
    val id: Int,
    val title: String,
    val description: String,
    val images: List<String> = emptyList()
) {
    val randomImageUrl: String?
        get() = images.randomOrNull()
}

@Serializable
data class ProductResponse(
    val products: List<Product>,
    val total: Int,
    val skip: Int,
    val limit: Int
)

/**
 * Model representing a completed Text Reading Task submission.
 */
@Serializable
@OptIn(ExperimentalTime::class)
data class TextReadingTask(
    override val taskId: String = generateTaskId(),
    override val taskType: String = "Text Reading",
    val text: String,
    val audioPath: String,
    override val durationSec: Int,
    // FIX: Change Instant to Long
    override val timestampMs: Long
) : AppTask

// Model representing a completed Image Description Task submission.
@Serializable
@OptIn(ExperimentalTime::class)
data class ImageDescriptionTask(
    override val taskId: String = generateTaskId(),
    override val taskType: String = "Image Description",
    val imageUrl: String,
    val audioPath: String,
    override val durationSec: Int,
    // FIX: Change Instant to Long
    override val timestampMs: Long
) : AppTask

// NEW DATA CLASS
/**
 * Model representing a completed Photo Capture Task submission.
 */
@Serializable
@OptIn(ExperimentalTime::class)
data class PhotoCaptureTask(
    override val taskId: String = generateTaskId(),
    override val taskType: String = "Photo Capture",
    val imagePath: String,
    val audioPath: String? = null,
    override val durationSec: Int? = null,
    val textDescription: String? = null,
    // FIX: Change Instant to Long
    override val timestampMs: Long
) : AppTask