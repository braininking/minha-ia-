package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AutonomyLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")
        val KEY_AUTONOMY_LEVEL = stringPreferencesKey("autonomy_level")
        val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        val KEY_SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val KEY_SPOKEN_RESPONSES = booleanPreferencesKey("spoken_responses")
        val KEY_FOREGROUND_SERVICE = booleanPreferencesKey("foreground_service")
        val KEY_FLOATING_BUBBLE = booleanPreferencesKey("floating_bubble")
        val KEY_CONTEXTUAL_MEMORY = booleanPreferencesKey("contextual_memory")
        val KEY_GEMINI_MODEL = stringPreferencesKey("gemini_model")
    }

    val aiProviderFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AI_PROVIDER] ?: "LOCAL_INTELLIGENT"
    }

    val autonomyLevelFlow: Flow<AutonomyLevel> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_AUTONOMY_LEVEL] ?: AutonomyLevel.EQUILIBRADO.name
        try {
            AutonomyLevel.valueOf(name)
        } catch (_: Exception) {
            AutonomyLevel.EQUILIBRADO
        }
    }

    val speechRateFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPEECH_RATE] ?: 1.0f
    }

    val speechPitchFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPEECH_PITCH] ?: 1.0f
    }

    val spokenResponsesFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPOKEN_RESPONSES] ?: true
    }

    val foregroundServiceFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_FOREGROUND_SERVICE] ?: false
    }

    val floatingBubbleFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_FLOATING_BUBBLE] ?: false
    }

    val contextualMemoryFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CONTEXTUAL_MEMORY] ?: true
    }

    val geminiModelFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GEMINI_MODEL] ?: "gemini-2.5-flash"
    }

    suspend fun setAiProvider(provider: String) {
        context.dataStore.edit { it[KEY_AI_PROVIDER] = provider }
    }

    suspend fun setAutonomyLevel(level: AutonomyLevel) {
        context.dataStore.edit { it[KEY_AUTONOMY_LEVEL] = level.name }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[KEY_SPEECH_RATE] = rate }
    }

    suspend fun setSpeechPitch(pitch: Float) {
        context.dataStore.edit { it[KEY_SPEECH_PITCH] = pitch }
    }

    suspend fun setSpokenResponses(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SPOKEN_RESPONSES] = enabled }
    }

    suspend fun setForegroundService(enabled: Boolean) {
        context.dataStore.edit { it[KEY_FOREGROUND_SERVICE] = enabled }
    }

    suspend fun setFloatingBubble(enabled: Boolean) {
        context.dataStore.edit { it[KEY_FLOATING_BUBBLE] = enabled }
    }

    suspend fun setContextualMemory(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CONTEXTUAL_MEMORY] = enabled }
    }

    suspend fun setGeminiModel(model: String) {
        context.dataStore.edit { it[KEY_GEMINI_MODEL] = model }
    }
}
