package com.example.ui

import java.util.UUID

enum class MessageSender {
    USER,
    ASSISTANT
}

enum class AppMode {
    VOICE,
    CHAT
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAction: Boolean = false,
    val actionName: String? = null,
    val actionDetail: String? = null,
    val imageBase64: String? = null
)
