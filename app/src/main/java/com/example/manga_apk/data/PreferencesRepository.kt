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
        private val AI_PROVIDER_KEY = stringPreferencesKey("ai_provider")
        private val API_KEY = stringPreferencesKey("api_key")
        private val CUSTOM_ENDPOINT_KEY = stringPreferencesKey("custom_endpoint")
        private val CUSTOM_MODEL_KEY = stringPreferencesKey("custom_model")
        private val TEXT_MODEL_KEY = stringPreferencesKey("text_model")
        private val VISION_MODEL_KEY = stringPreferencesKey("vision_model")
        private val INCLUDE_GRAMMAR_KEY = booleanPreferencesKey("include_grammar")
        private val INCLUDE_VOCABULARY_KEY = booleanPreferencesKey("include_vocabulary")
        private val INCLUDE_TRANSLATION_KEY = booleanPreferencesKey("include_translation")
    }
    
    val aiConfigFlow: Flow<AIConfig> = context.dataStore.data
        .map { preferences ->
            AIConfig(
                provider = AIProvider.valueOf(
                    preferences[AI_PROVIDER_KEY] ?: AIProvider.OPENAI.name
                ),
                apiKey = preferences[API_KEY] ?: "",
                customEndpoint = preferences[CUSTOM_ENDPOINT_KEY] ?: "",
                customModel = preferences[CUSTOM_MODEL_KEY] ?: "",
                textModel = preferences[TEXT_MODEL_KEY] ?: "gpt-4-turbo",
                visionModel = preferences[VISION_MODEL_KEY] ?: "gpt-4-vision-preview",
                includeGrammar = preferences[INCLUDE_GRAMMAR_KEY] ?: true,
                includeVocabulary = preferences[INCLUDE_VOCABULARY_KEY] ?: true,
                includeTranslation = preferences[INCLUDE_TRANSLATION_KEY] ?: true
            )
        }
    
    suspend fun saveAIConfig(config: AIConfig) {
        context.dataStore.edit { preferences ->
            preferences[AI_PROVIDER_KEY] = config.provider.name
            preferences[API_KEY] = config.apiKey
            preferences[CUSTOM_ENDPOINT_KEY] = config.customEndpoint
            preferences[CUSTOM_MODEL_KEY] = config.customModel
            preferences[TEXT_MODEL_KEY] = config.textModel
            preferences[VISION_MODEL_KEY] = config.visionModel
            preferences[INCLUDE_GRAMMAR_KEY] = config.includeGrammar
            preferences[INCLUDE_VOCABULARY_KEY] = config.includeVocabulary
            preferences[INCLUDE_TRANSLATION_KEY] = config.includeTranslation
        }
    }
}