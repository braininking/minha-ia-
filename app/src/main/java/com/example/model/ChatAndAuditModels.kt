package com.example.model

import java.util.UUID

enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val associatedTaskId: String? = null,
    val planSummary: String? = null,
    val isSpoken: Boolean = false
)

data class AuditLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val target: String,
    val result: String,
    val level: String,
    val taskId: String? = null
)
