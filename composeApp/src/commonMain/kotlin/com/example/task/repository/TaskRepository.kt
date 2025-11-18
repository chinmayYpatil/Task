package com.example.task.repository

import com.example.task.data.ProductResponse
import com.example.task.data.TextReadingTask
import com.example.task.data.ImageDescriptionTask
import com.example.task.data.PhotoCaptureTask
import com.example.task.data.AppTask
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import io.ktor.client.* import io.ktor.client.call.* import io.ktor.client.request.* import io.ktor.client.plugins.contentnegotiation.* import io.ktor.serialization.kotlinx.json.* import io.ktor.http.* import kotlinx.serialization.builtins.ListSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer

// Setup Ktor HTTP Client and JSON configuration
private val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        })
    }
}

// FIX: Custom Json instance with sealed interface for polymorphic serialization
@OptIn(ExperimentalSerializationApi::class)
private val taskJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    isLenient = true
    serializersModule = SerializersModule {
        // Register sealed class AppTask and its concrete implementations
        polymorphic(AppTask::class) {
            subclass(TextReadingTask::class)
            subclass(ImageDescriptionTask::class)
            subclass(PhotoCaptureTask::class)
        }
    }
}


// FIX: EXPECT: Interface for platform-specific file I/O for task history
internal expect object TaskStorage {
    fun saveTasksJson(jsonString: String)
    fun loadTasksJson(): String?
}

// Mock class to simulate network/data operations
class TaskRepository {

    private val completedTasks = mutableListOf<AppTask>()

    init {
        // Load tasks from disk on initialization
        loadTasksFromDisk()
    }

    private fun loadTasksFromDisk() {
        val jsonString = TaskStorage.loadTasksJson()
        if (jsonString != null) {
            try {
                // FIX: Use the custom Json instance and the ListSerializer for AppTask
                @OptIn(InternalSerializationApi::class)
                val loadedTasks: List<AppTask> = taskJson.decodeFromString(
                    ListSerializer(AppTask::class.serializer()),
                    jsonString
                )
                completedTasks.addAll(loadedTasks)
                println("Tasks loaded successfully: ${completedTasks.size}")
            } catch (e: Exception) {
                println("Error loading tasks from disk: ${e.message}")
            }
        }
    }

    private suspend fun saveTasksToDisk() {
        withContext(Dispatchers.Default) {
            try {
                // FIX: Use the custom Json instance and the ListSerializer for AppTask
                @OptIn(InternalSerializationApi::class)
                val jsonString = taskJson.encodeToString(
                    ListSerializer(AppTask::class.serializer()),
                    completedTasks
                )
                // Save to platform storage
                TaskStorage.saveTasksJson(jsonString)
            } catch (e: Exception) {
                println("Error saving tasks to disk: ${e.message}")
            }
        }
    }

    suspend fun fetchCompletedTasks(): List<AppTask> {
        delay(200L)
        return completedTasks.toList()
    }

    suspend fun fetchTextPassage(): String {
        return try {
            val apiUrl = "https://dummyjson.com/products"
            val response: ProductResponse = httpClient.get(apiUrl).body()
            response.products.randomOrNull()?.description ?: "Error: No products found in API response."
        } catch (e: Exception) {
            "The Eyeshadow Palette with Mirror offers a versatile range of eyeshadow shades for creating stunning eye looks. With a built-in mirror, it's convenient for on-the-go makeup application."
        }
    }

    suspend fun fetchImageDescriptionTaskData(): Pair<String, String?> {
        return try {
            val apiUrl = "https://dummyjson.com/products"
            val response: ProductResponse = httpClient.get(apiUrl).body()
            val randomProduct = response.products.randomOrNull()
            val imageUrl = randomProduct?.randomImageUrl ?: "https://cdn.dummyjson.com/product-images/14/2.jpg"
            val instruction = randomProduct?.title?.let { "Describe the image of '${it}' in your native language." } ?: "Describe what you see in your native language."
            instruction to imageUrl
        } catch (e: Exception) {
            "Describe the image you see in your native language." to "https://cdn.dummyjson.com/product-images/14/2.jpg"
        }
    }

    suspend fun saveTextReadingTask(task: TextReadingTask) {
        delay(100L)
        completedTasks.add(task)
        saveTasksToDisk() // Persist
        println("TASK SAVED: $task")
    }

    suspend fun saveImageDescriptionTask(task: ImageDescriptionTask) {
        delay(100L)
        completedTasks.add(task)
        saveTasksToDisk() // Persist
        println("TASK SAVED: $task")
    }

    suspend fun savePhotoCaptureTask(task: PhotoCaptureTask) {
        delay(100L)
        completedTasks.add(task)
        saveTasksToDisk() // Persist
        println("TASK SAVED: $task")
    }
}