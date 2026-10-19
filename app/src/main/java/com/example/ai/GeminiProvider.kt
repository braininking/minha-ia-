package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.model.ChatMessage
import com.example.model.InstalledAppInfo
import com.example.model.ScreenContext
import com.example.model.TaskPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiProvider(
    private val localFallback: LocalIntelligentProvider = LocalIntelligentProvider()
) : AIProvider {

    override val providerId: String = "GEMINI_API"
    override val displayName: String = "Google Gemini API (Cloud Inteligente)"

    override val isConfigured: Boolean
        get() {
            return try {
                val key = getApiKey()
                key.isNotBlank() && key != "MY_GEMINI_API_KEY"
            } catch (_: Exception) {
                false
            }
        }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            // Checked via BuildConfig if injected by Secrets Gradle Plugin
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String)?.trim() ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    override suspend fun generateChatResponse(
        prompt: String,
        history: List<ChatMessage>
    ): Result<String> {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Graceful offline fallback with helpful note
            val fallbackResult = localFallback.generateChatResponse(prompt, history)
            return fallbackResult.map { reply ->
                "$reply\n\n[Nota: Operando via Motor Local Inteligente. Para ativar a nuvem Gemini, configure a chave no painel Secrets.]"
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

                val contentsArray = JSONArray()
                for (msg in history.takeLast(6)) {
                    val role = if (msg.sender == com.example.model.MessageSender.USER) "user" else "model"
                    val partObj = JSONObject().put("text", msg.text)
                    val contentObj = JSONObject()
                        .put("role", role)
                        .put("parts", JSONArray().put(partObj))
                    contentsArray.put(contentObj)
                }

                // Add current prompt
                val curPart = JSONObject().put("text", prompt)
                val curContent = JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(curPart))
                contentsArray.put(curContent)

                val systemInstruction = JSONObject().put(
                    "parts",
                    JSONArray().put(
                        JSONObject().put(
                            "text",
                            "Você é o Gemini Mobile AI, um assistente avançado para Android. Responda em português brasileiro de forma concisa, elegante, técnica e prestativa. Foque em ajudar com controle do celular, automação de tarefas e produtividade."
                        )
                    )
                )

                val bodyJson = JSONObject()
                    .put("contents", contentsArray)
                    .put("systemInstruction", systemInstruction)

                val request = Request.Builder()
                    .url(url)
                    .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        Log.e("GeminiProvider", "API error: ${response.code} $errBody")
                        return@withContext localFallback.generateChatResponse(prompt, history)
                    }

                    val respString = response.body?.string() ?: ""
                    val root = JSONObject(respString)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val text = candidates.getJSONObject(0)
                            .optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text") ?: ""
                        Result.success(text)
                    } else {
                        localFallback.generateChatResponse(prompt, history)
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiProvider", "Exception in GeminiProvider: ${e.message}", e)
                localFallback.generateChatResponse(prompt, history)
            }
        }
    }

    override suspend fun planTask(
        userGoal: String,
        installedApps: List<InstalledAppInfo>,
        screenContext: ScreenContext?
    ): Result<TaskPlan> {
        // High-level planning with local safety verification
        return localFallback.planTask(userGoal, installedApps, screenContext)
    }
}
