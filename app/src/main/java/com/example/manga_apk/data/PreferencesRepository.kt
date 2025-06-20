package com.example.manga_apk.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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
                    apiKey = preferences[OPENAI_API_KEY] ?: "",
                    textModel = preferences[OPENAI_TEXT_MODEL_KEY] ?: "gpt-4-turbo",
                    visionModel = preferences[OPENAI_VISION_MODEL_KEY] ?: "gpt-4o"
                ),
                geminiConfig = GeminiConfig(
                    apiKey = preferences[GEMINI_API_KEY] ?: "",
                    model = preferences[GEMINI_MODEL_KEY] ?: "gemini-1.5-pro"
                ),
                customConfig = CustomAPIConfig(
                    apiKey = preferences[CUSTOM_API_KEY] ?: "",
                    endpoint = preferences[CUSTOM_ENDPOINT_KEY] ?: "",
                    model = preferences[CUSTOM_MODEL_KEY] ?: ""
                ),
                includeGrammar = preferences[INCLUDE_GRAMMAR_KEY] ?: true,
                includeVocabulary = preferences[INCLUDE_VOCABULARY_KEY] ?: true,
                includeTranslation = preferences[INCLUDE_TRANSLATION_KEY] ?: true
            )
            println("PreferencesRepository: Loaded AI config - Primary Provider: ${config.primaryProvider}, Fallback enabled: ${config.enableFallback}")
            config
        }
    
    suspend fun saveAIConfig(config: AIConfig) {
        context.dataStore.edit { preferences ->
            preferences[PRIMARY_PROVIDER_KEY] = config.primaryProvider.name
            preferences[ENABLE_FALLBACK_KEY] = config.enableFallback
            
            // OpenAI Config
            preferences[OPENAI_API_KEY] = config.openaiConfig.apiKey
            preferences[OPENAI_TEXT_MODEL_KEY] = config.openaiConfig.textModel
            preferences[OPENAI_VISION_MODEL_KEY] = config.openaiConfig.visionModel
            
            // Gemini Config
            preferences[GEMINI_API_KEY] = config.geminiConfig.apiKey
            preferences[GEMINI_MODEL_KEY] = config.geminiConfig.model
            
            // Custom API Config
            preferences[CUSTOM_API_KEY] = config.customConfig.apiKey
            preferences[CUSTOM_ENDPOINT_KEY] = config.customConfig.endpoint
            preferences[CUSTOM_MODEL_KEY] = config.customConfig.model
            
            // General settings
            preferences[INCLUDE_GRAMMAR_KEY] = config.includeGrammar
            preferences[INCLUDE_VOCABULARY_KEY] = config.includeVocabulary
            preferences[INCLUDE_TRANSLATION_KEY] = config.includeTranslation
        }
    }
}