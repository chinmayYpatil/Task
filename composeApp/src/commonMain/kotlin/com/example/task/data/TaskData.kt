package com.example.task.data

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable // NEW: Mark class for serialization
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

@Serializable // NEW: Mark class for serialization
data class ProductResponse(
    val products: List<Product>,
    val total: Int,
    val skip: Int,
    val limit: Int
)

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

// Model representing a completed Image Description Task submission.
data class ImageDescriptionTask @OptIn(ExperimentalTime::class) constructor(
    val taskType: String = "image_description",
    val imageUrl: String, // The URL of the image the user described
    val audioPath: String,
    val durationSec: Int,
    val timestamp: Instant
)

// NEW DATA CLASS
/**
 * Model representing a completed Photo Capture Task submission.
 */
data class PhotoCaptureTask @OptIn(ExperimentalTime::class) constructor(
    val taskType: String = "photo_capture",
    val imagePath: String, // The local path of the captured photo
    val audioPath: String? = null, // Optional audio description path
    val durationSec: Int? = null, // Optional audio duration
    val textDescription: String? = null, // Optional text description
    val timestamp: Instant
)