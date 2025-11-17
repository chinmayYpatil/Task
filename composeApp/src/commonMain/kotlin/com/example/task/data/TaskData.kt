package com.example.task.data

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.serialization.Serializable // NEW IMPORT
// NOTE: These classes are required for Kotlinx Serialization to parse the API response

@Serializable // NEW: Mark class for serialization
data class Product(
    val id: Int,
    val title: String,
    val description: String // This is the field we want to extract
)

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