package com.example.ai

import com.example.model.ChatMessage
import com.example.model.InstalledAppInfo
import com.example.model.ScreenContext
import com.example.model.TaskPlan

interface AIProvider {
    val providerId: String
    val displayName: String
    val isConfigured: Boolean

    suspend fun generateChatResponse(
        prompt: String,
        history: List<ChatMessage>
    ): Result<String>

    suspend fun planTask(
        userGoal: String,
        installedApps: List<InstalledAppInfo>,
        screenContext: ScreenContext?
    ): Result<TaskPlan>
}
