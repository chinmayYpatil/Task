package com.example.task.data

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.time.Clock // Explicit import

// Utility to generate a pseudo-unique ID for KMP based on timestamp
@OptIn(ExperimentalTime::class)
private fun generateTaskId(): String = Clock.System.now().toEpochMilliseconds().toString()

// Define a common sealed interface for all tasks
sealed interface AppTask {
    val taskId: String
    val taskType: String
    @OptIn(ExperimentalTime::class)
    val timestamp: Instant
    val durationSec: Int? // Use nullable duration for PhotoCapture
}


@Serializable
data class Product(
    val id: Int,
    val title: String,
    val description: String,
    // NEW: Add images field to fetch image URLs
    val images: List<String> = emptyList()
) {
    // Helper property to get a random image URL for the task
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
@OptIn(ExperimentalTime::class)
data class TextReadingTask(
    override val taskId: String = generateTaskId(), // Added ID
    override val taskType: String = "Text Reading", // Changed to readable string
    val text: String,
    val audioPath: String,
    override val durationSec: Int,
    override val timestamp: Instant // Using Instant for timestamp
) : AppTask

// Model representing a completed Image Description Task submission.
@OptIn(ExperimentalTime::class)
data class ImageDescriptionTask(
    override val taskId: String = generateTaskId(), // Added ID
    override val taskType: String = "Image Description", // Changed to readable string
    val imageUrl: String, // The URL of the image the user described
    val audioPath: String,
    override val durationSec: Int,
    override val timestamp: Instant
) : AppTask

// NEW DATA CLASS
/**
 * Model representing a completed Photo Capture Task submission.
 */
@OptIn(ExperimentalTime::class)
data class PhotoCaptureTask(
    override val taskId: String = generateTaskId(), // Added ID
    override val taskType: String = "Photo Capture", // Changed to readable string
    val imagePath: String, // The local path of the captured photo
    val audioPath: String? = null, // Optional audio description path
    override val durationSec: Int? = null, // Optional audio duration
    val textDescription: String? = null, // Optional text description
    override val timestamp: Instant
) : AppTask