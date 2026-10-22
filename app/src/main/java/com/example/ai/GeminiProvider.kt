package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.model.ActionType
import com.example.model.ChatMessage
import com.example.model.InstalledAppInfo
import com.example.model.ScreenContext
import com.example.model.TaskPlan
import com.example.model.TaskStep
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

    suspend fun testConnection(): Result<String> {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return Result.failure(IllegalStateException(
                "Chave Gemini não configurada. Configure GEMINI_API_KEY no Secrets do AI Studio ou em .env."
            ))
        }

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Result.success("Conexão Gemini confirmada; a chave foi aceita pela API.")
                    } else {
                        Log.w("GeminiProvider", "Teste de conexão Gemini falhou (HTTP ${response.code}).")
                        Result.failure(IllegalStateException(
                            "Gemini respondeu HTTP ${response.code}. Confira a chave e as permissões da API."
                        ))
                    }
                }
            } catch (e: Exception) {
                Log.w("GeminiProvider", "Não foi possível testar a conexão Gemini.", e)
                Result.failure(IllegalStateException(
                    "Falha de rede ao testar o Gemini: ${e.localizedMessage ?: "conexão indisponível"}"
                ))
            }
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
        val explicitMessageCommand =
            Regex("\\b(envie|envia|enviar|mande|manda|mandar|send)\\b", RegexOption.IGNORE_CASE)
                .containsMatchIn(userGoal) &&
                (userGoal.contains("whatsapp", ignoreCase = true) ||
                    userGoal.contains("mensagem", ignoreCase = true))
        if (explicitMessageCommand) {
            // Use the deterministic accessibility flow for side-effectful messaging;
            // Gemini must not invent a shortcut that skips contact/draft verification.
            return localFallback.planTask(userGoal, installedApps, screenContext)
        }

        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return localFallback.planTask(userGoal, installedApps, screenContext)
        }

        return withContext(Dispatchers.IO) {
            try {
                // Send only the active package, not visible screen text that may contain private data.
                val appCatalog = installedApps
                    .take(80)
                    .joinToString("\n") { "- ${it.appName.take(80)} | ${it.packageName}" }
                val prompt = """
                    Crie um plano de automação Android para o comando do usuário.
                    Responda SOMENTE com JSON válido neste formato:
                    {"steps":[{"actionType":"LAUNCH_APP","description":"...","targetPackage":"...","targetLabel":"...","targetText":"...","targetQuery":"..."}]}
                    Campos sem uso devem ser omitidos ou ser strings vazias.
                    Ações permitidas: LAUNCH_APP, OPEN_URL, ACCESSIBILITY_CLICK, ACCESSIBILITY_INPUT,
                    ACCESSIBILITY_SCROLL, SEND_WHATSAPP_MESSAGE, SEARCH_WEB, WAIT_DELAY, VERIFY_RESULT.
                    Nunca crie ações fora dessa lista. Para mensagens do WhatsApp, use o fluxo local
                    determinístico; não tente enviar por intents genéricos nem invente confirmação de entrega.
                    Use somente pacotes da lista de apps quando abrir um aplicativo. Para cliques, use
                    rótulos exatos e necessários. Não inclua etapas de envio, compra, exclusão, publicação
                    ou submissão automática. No máximo 25 etapas. Se o comando não puder ser convertido
                    com segurança, retorne {"steps":[]}.

                    Comando do usuário:
                    ${userGoal.take(2000)}

                    Pacote da tela atual: ${screenContext?.packageName ?: "desconhecido"}

                    Aplicativos instalados:
                    ${appCatalog.ifBlank { "Nenhum catálogo de apps disponível." }}
                """.trimIndent()

                val bodyJson = JSONObject()
                    .put("contents", JSONArray().put(
                        JSONObject().put("role", "user").put(
                            "parts", JSONArray().put(JSONObject().put("text", prompt))
                        )
                    ))
                    .put("generationConfig", JSONObject().put("responseMimeType", "application/json"))

                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        Log.w("GeminiProvider", "Falha no planejamento Gemini (HTTP ${response.code}); usando motor local.")
                        return@withContext localFallback.planTask(userGoal, installedApps, screenContext)
                    }

                    val responseJson = JSONObject(responseBody)
                    val modelText = responseJson.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        .orEmpty()
                        .trim()
                    val cleanJson = modelText
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                    val stepsJson = JSONObject(cleanJson).optJSONArray("steps")
                        ?: return@withContext localFallback.planTask(userGoal, installedApps, screenContext)

                    if (stepsJson.length() == 0 || stepsJson.length() > 25) {
                        return@withContext localFallback.planTask(userGoal, installedApps, screenContext)
                    }

                    val steps = mutableListOf<TaskStep>()
                    for (index in 0 until stepsJson.length()) {
                        val item = stepsJson.optJSONObject(index)
                            ?: return@withContext localFallback.planTask(userGoal, installedApps, screenContext)
                        val actionType = runCatching {
                            ActionType.valueOf(item.optString("actionType").uppercase())
                        }.getOrNull()
                            ?: return@withContext localFallback.planTask(userGoal, installedApps, screenContext)

                        fun optionalText(key: String): String? =
                            item.optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }

                        steps.add(
                            TaskStep(
                                index = index,
                                actionType = actionType,
                                description = (optionalText("description") ?: actionType.labelPt).take(240),
                                targetPackage = optionalText("targetPackage")?.take(200),
                                targetLabel = optionalText("targetLabel")?.take(200),
                                targetText = optionalText("targetText")?.take(2000),
                                targetQuery = optionalText("targetQuery")?.take(1000),
                                // The local AIPlanner replaces this with policy-enforced values.
                                requiresConfirmation = true
                            )
                        )
                    }

                    Result.success(TaskPlan(goal = userGoal, steps = steps))
                }
            } catch (e: Exception) {
                Log.w("GeminiProvider", "Planejamento Gemini indisponível; usando motor local.", e)
                localFallback.planTask(userGoal, installedApps, screenContext)
            }
        }
    }
}
