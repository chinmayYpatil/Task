package com.example.task.repository

import com.example.task.data.Product
import com.example.task.data.ProductResponse
import com.example.task.data.TextReadingTask
import com.example.task.data.ImageDescriptionTask
import com.example.task.data.PhotoCaptureTask
import com.example.task.data.AppTask
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import io.ktor.client.* import io.ktor.client.call.* import io.ktor.client.request.* import io.ktor.client.plugins.contentnegotiation.* import io.ktor.serialization.kotlinx.json.* import io.ktor.http.* import kotlin.random.Random

// Setup Ktor HTTP Client and JSON configuration
// This initialization block needs to be done once in a real KMP project.
private val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true // Ignore fields we don't define in our data classes
            prettyPrint = true
            isLenient = true
        })
    }
}

// Mock class to simulate network/data operations
class TaskRepository {

    // FIX: Move the in-memory store to a companion object to make it a singleton instance
    // shared across all instances of TaskRepository created by ViewModels.
    companion object {
        private val completedTasks = mutableListOf<AppTask>()
    }

    /**
     * Retrieves all locally saved tasks.
     */
    suspend fun fetchCompletedTasks(): List<AppTask> {
        delay(200L) // Simulate network/disk read delay
        // FIX: Reference the companion object's shared list
        return completedTasks.toList()
    }

    /**
     * Fetches the product list from the API and extracts the **random** product's description.
     */
    suspend fun fetchTextPassage(): String {
        return try {
            val apiUrl = "https://dummyjson.com/products"

            // Perform the network request
            val response: ProductResponse = httpClient.get(apiUrl).body()

            // FIX: Extract the description of a RANDOM product instead of the first one
            response.products.randomOrNull()?.description
                ?: "Error: No products found in API response."
        } catch (e: Exception) {
            // Log the error and return a fallback text
            println("API Fetch Error: ${e.message}")
            // Fallback to a hardcoded description if the network or parsing fails
            "The Eyeshadow Palette with Mirror offers a versatile range of eyeshadow shades for creating stunning eye looks. With a built-in mirror, it's convenient for on-the-go makeup application."
        }
    }

    /**
     * Fetches a random product and extracts the image URL for the Image Description task.
     * @return Pair<String, String?> of (Title/Instruction, Image URL)
     */
    suspend fun fetchImageDescriptionTaskData(): Pair<String, String?> {
        return try {
            val apiUrl = "https://dummyjson.com/products"

            // Perform the network request
            val response: ProductResponse = httpClient.get(apiUrl).body()

            val randomProduct = response.products.randomOrNull()

            val imageUrl = randomProduct?.randomImageUrl // Use the helper property
            // Fallback to a hardcoded image if fetch fails or product has no images
                ?: "https://cdn.dummyjson.com/product-images/14/2.jpg"

            val instruction = randomProduct?.title?.let { "Describe the image of '${it}' in your native language." }
                ?: "Describe what you see in your native language."

            instruction to imageUrl

        } catch (e: Exception) {
            println("API Fetch Error for Image Task: ${e.message}")
            // Fallback text and hardcoded image
            "Describe the image you see in your native language." to "https://cdn.dummyjson.com/product-images/14/2.jpg"
        }
    }

    // Mock function to simulate saving the Text Reading task locally
    suspend fun saveTextReadingTask(task: TextReadingTask) {
        delay(100L) // Simulate disk write delay
        completedTasks.add(task) // Store the task
        println("TASK SAVED: $task")
    }

    // Mock function to simulate saving the Image Description task locally
    suspend fun saveImageDescriptionTask(task: ImageDescriptionTask) {
        delay(100L) // Simulate disk write delay
        completedTasks.add(task) // Store the task
        println("TASK SAVED: $task")
    }

    // NEW FUNCTION
    // Mock function to simulate saving the Photo Capture task locally
    suspend fun savePhotoCaptureTask(task: PhotoCaptureTask) {
        delay(100L) // Simulate disk write delay
        completedTasks.add(task) // Store the task
        println("TASK SAVED: $task")
    }
}