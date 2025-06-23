package com.example.manga_apk.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {
    
    companion object {
        private val PRIMARY_PROVIDER_KEY = stringPreferencesKey("primary_provider")
        private val ENABLE_FALLBACK_KEY = booleanPreferencesKey("enable_fallback")
        
        // OpenAI Config
        private val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        private val OPENAI_TEXT_MODEL_KEY = stringPreferencesKey("openai_text_model")
        private val OPENAI_VISION_MODEL_KEY = stringPreferencesKey("openai_vision_model")
        
        // Gemini Config
        private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val GEMINI_MODEL_KEY = stringPreferencesKey("gemini_model")
        
        // Custom API Config
        private val CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
        private val CUSTOM_ENDPOINT_KEY = stringPreferencesKey("custom_endpoint")
        private val CUSTOM_MODEL_KEY = stringPreferencesKey("custom_model")
        
        // General settings
        private val INCLUDE_GRAMMAR_KEY = booleanPreferencesKey("include_grammar")
        private val INCLUDE_VOCABULARY_KEY = booleanPreferencesKey("include_vocabulary")
        private val INCLUDE_TRANSLATION_KEY = booleanPreferencesKey("include_translation")
    }
    
    val aiConfigFlow: Flow<AIConfig> = context.dataStore.data
        .map { preferences ->
            // Load and trim API keys to remove any whitespace
            val openaiKey = (preferences[OPENAI_API_KEY] ?: "").trim()
            val geminiKey = (preferences[GEMINI_API_KEY] ?: "").trim()
            val customKey = (preferences[CUSTOM_API_KEY] ?: "").trim()
            val customEndpoint = (preferences[CUSTOM_ENDPOINT_KEY] ?: "").trim()
            
            val config = AIConfig(
                primaryProvider = try {
                    AIProvider.valueOf(
                        preferences[PRIMARY_PROVIDER_KEY] ?: AIProvider.OPENAI.name
                    )
                } catch (e: IllegalArgumentException) {
                    AIProvider.OPENAI // fallback to OPENAI if invalid provider name
                },
                enableFallback = preferences[ENABLE_FALLBACK_KEY] ?: false,
                openaiConfig = OpenAIConfig(
                    apiKey = openaiKey,
                    textModel = (preferences[OPENAI_TEXT_MODEL_KEY] ?: "gpt-4-turbo").trim(),
                    visionModel = (preferences[OPENAI_VISION_MODEL_KEY] ?: "gpt-4o").trim()
                ),
                geminiConfig = GeminiConfig(
                    apiKey = geminiKey,
                    model = (preferences[GEMINI_MODEL_KEY] ?: "gemini-1.5-pro").trim()
                ),
                customConfig = CustomAPIConfig(
                    apiKey = customKey,
                    endpoint = customEndpoint,
                    model = (preferences[CUSTOM_MODEL_KEY] ?: "").trim()
                ),
                includeGrammar = preferences[INCLUDE_GRAMMAR_KEY] ?: true,
                includeVocabulary = preferences[INCLUDE_VOCABULARY_KEY] ?: true,
                includeTranslation = preferences[INCLUDE_TRANSLATION_KEY] ?: true
            )
            
            // Enhanced debug logging
            println("PreferencesRepository: Loaded AI config - Primary Provider: ${config.primaryProvider}, Fallback enabled: ${config.enableFallback}")
            println("PreferencesRepository: OpenAI key length: ${openaiKey.length}, Gemini key length: ${geminiKey.length}, Custom key length: ${customKey.length}")
            println("PreferencesRepository: Configured providers: ${config.getConfiguredProviders()}")
            android.util.Log.d("MangaLearnJP", "PreferencesRepository: API Keys - OpenAI: ${if (openaiKey.isNotEmpty()) "${openaiKey.length} chars" else "empty"}, Gemini: ${if (geminiKey.isNotEmpty()) "${geminiKey.length} chars" else "empty"}, Custom: ${if (customKey.isNotEmpty()) "${customKey.length} chars" else "empty"}")
            
            config
        }
    
    suspend fun saveAIConfig(config: AIConfig) {
        // Pre-trim all values to ensure consistent storage
        val trimmedOpenaiKey = config.openaiConfig.apiKey.trim()
        val trimmedGeminiKey = config.geminiConfig.apiKey.trim()
        val trimmedCustomKey = config.customConfig.apiKey.trim()
        val trimmedCustomEndpoint = config.customConfig.endpoint.trim()
        
        println("PreferencesRepository: saveAIConfig called - OpenAI key length: ${trimmedOpenaiKey.length}, Gemini key length: ${trimmedGeminiKey.length}")
        android.util.Log.d("MangaLearnJP", "PreferencesRepository: saveAIConfig called - OpenAI key length: ${trimmedOpenaiKey.length}, Gemini key length: ${trimmedGeminiKey.length}")
        
        try {
            context.dataStore.edit { preferences ->
                preferences[PRIMARY_PROVIDER_KEY] = config.primaryProvider.name
                preferences[ENABLE_FALLBACK_KEY] = config.enableFallback
                
                // OpenAI Config
                preferences[OPENAI_API_KEY] = trimmedOpenaiKey
                preferences[OPENAI_TEXT_MODEL_KEY] = config.openaiConfig.textModel.trim()
                preferences[OPENAI_VISION_MODEL_KEY] = config.openaiConfig.visionModel.trim()
                
                // Gemini Config
                preferences[GEMINI_API_KEY] = trimmedGeminiKey
                preferences[GEMINI_MODEL_KEY] = config.geminiConfig.model.trim()
                
                // Custom API Config
                preferences[CUSTOM_API_KEY] = trimmedCustomKey
                preferences[CUSTOM_ENDPOINT_KEY] = trimmedCustomEndpoint
                preferences[CUSTOM_MODEL_KEY] = config.customConfig.model.trim()
                
                // General settings
                preferences[INCLUDE_GRAMMAR_KEY] = config.includeGrammar
                preferences[INCLUDE_VOCABULARY_KEY] = config.includeVocabulary
                preferences[INCLUDE_TRANSLATION_KEY] = config.includeTranslation
            }
            
            // Verification after saving can be performed elsewhere if needed to avoid extra suspension and overhead.
        } catch (e: Exception) {
            println("PreferencesRepository: ERROR saving config: ${e.message}")
            android.util.Log.e("MangaLearnJP", "Error saving AI config to DataStore", e)
            throw e // Rethrow to allow caller to handle
        }
    }
    
    suspend fun clearAllPreferences() {
        println("PreferencesRepository: Clearing all preferences")
        android.util.Log.d("MangaLearnJP", "PreferencesRepository: Clearing all preferences")
        
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
        
        println("PreferencesRepository: All preferences cleared")
        android.util.Log.d("MangaLearnJP", "PreferencesRepository: All preferences cleared")
    }
}