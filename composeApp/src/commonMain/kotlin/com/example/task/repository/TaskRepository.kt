package com.example.task.repository

import com.example.task.data.TextReadingTask
import kotlinx.coroutines.delay

// Mock class to simulate network/data operations
class TaskRepository {

    // Mock API call function to get a product description (from dummyjson.com JSON)
    suspend fun fetchTextPassage(): String {
        delay(300L) // Simulate network delay
        // Using the description of product #2: "Eyeshadow Palette with Mirror"
        return "The Eyeshadow Palette with Mirror offers a versatile range of eyeshadow shades for creating stunning eye looks. With a built-in mirror, it's convenient for on-the-go makeup application."
    }

    // Mock function to simulate saving the task locally
    suspend fun saveTextReadingTask(task: TextReadingTask) {
        delay(100L) // Simulate disk write delay
        println("TASK SAVED: $task")
    }
}