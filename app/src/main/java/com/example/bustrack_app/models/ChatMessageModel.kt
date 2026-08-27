package com.example.bustrack_app.models

data class ChatMessageModel(
    val role: String,   // "user" or "assistant"
    val content: String,
    val isError: Boolean = false
)
