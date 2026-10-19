package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val associatedTaskId: String?,
    val planSummary: String?
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val goal: String,
    val stepsJson: String,
    val currentStepIndex: Int,
    val status: String,
    val createdAt: Long,
    val finishedAt: Long?,
    val autonomyLevel: String,
    val errorMessage: String?
)

@Entity(tableName = "audit_logs")
data class AuditEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val action: String,
    val target: String,
    val result: String,
    val level: String,
    val taskId: String?
)
