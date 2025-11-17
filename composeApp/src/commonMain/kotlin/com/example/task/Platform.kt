package com.example.task

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform