package com.example.task.repository

import com.example.task.data.ProductResponse
import com.example.task.data.TextReadingTask
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import io.ktor.client.* import io.ktor.client.call.* import io.ktor.client.request.* import io.ktor.client.plugins.contentnegotiation.* import io.ktor.serialization.kotlinx.json.* import io.ktor.http.* import kotlin.random.Random // NEW IMPORT: To ensure Random is available

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

    // Mock function to simulate saving the task locally
    suspend fun saveTextReadingTask(task: TextReadingTask) {
        delay(100L) // Simulate disk write delay
        println("TASK SAVED: $task")
    }
}