package com.example.manga_apk.data

import android.graphics.Bitmap

enum class AIProvider(val displayName: String) {
    OPENAI("OpenAI GPT-4 Vision"),
    GEMINI("Google Gemini Vision"),
    CUSTOM("Custom OpenAI-Format API")
}

data class OpenAIConfig(
    val apiKey: String = "",
    val textModel: String = "gpt-4-turbo",
    val visionModel: String = "gpt-4o"
)

data class GeminiConfig(
    val apiKey: String = "",
    val model: String = "gemini-1.5-pro"
)

data class CustomAPIConfig(
    val apiKey: String = "",
    val endpoint: String = "",
    val model: String = ""
)

data class AIConfig(
    val primaryProvider: AIProvider = AIProvider.OPENAI,
    val enableFallback: Boolean = false,
    val openaiConfig: OpenAIConfig = OpenAIConfig(),
    val geminiConfig: GeminiConfig = GeminiConfig(),
    val customConfig: CustomAPIConfig = CustomAPIConfig(),
    val includeGrammar: Boolean = true,
    val includeVocabulary: Boolean = true,
    val includeTranslation: Boolean = true
) {
    fun getConfiguredProviders(): List<AIProvider> {
        val providers = mutableListOf<AIProvider>()
        
        // Add primary provider first
        providers.add(primaryProvider)
        
        // Add other configured providers if fallback is enabled
        if (enableFallback) {
            if (primaryProvider != AIProvider.OPENAI && openaiConfig.apiKey.isNotEmpty()) {
                providers.add(AIProvider.OPENAI)
            }
            if (primaryProvider != AIProvider.GEMINI && geminiConfig.apiKey.isNotEmpty()) {
                providers.add(AIProvider.GEMINI)
            }
            if (primaryProvider != AIProvider.CUSTOM && customConfig.apiKey.isNotEmpty() && customConfig.endpoint.isNotEmpty()) {
                providers.add(AIProvider.CUSTOM)
            }
        }
        
        return providers
    }
    
    fun isProviderConfigured(provider: AIProvider): Boolean {
        return when (provider) {
            AIProvider.OPENAI -> openaiConfig.apiKey.isNotEmpty()
            AIProvider.GEMINI -> geminiConfig.apiKey.isNotEmpty()
            AIProvider.CUSTOM -> customConfig.apiKey.isNotEmpty() && customConfig.endpoint.isNotEmpty()
        }
    }
}

data class VocabularyItem(
    val word: String,
    val reading: String,
    val meaning: String,
    val partOfSpeech: String,
    val jlptLevel: String? = null,
    val difficulty: Int = 1
)

data class GrammarPattern(
    val pattern: String,
    val explanation: String,
    val example: String,
    val difficulty: String
)

data class TextAnalysis(
    val originalText: String,
    val vocabulary: List<VocabularyItem>,
    val grammarPatterns: List<GrammarPattern>,
    val translation: String,
    val context: String
)

data class PanelSegment(
    val id: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val readingOrder: Int,
    val extractedText: String = "",
    val analysis: TextAnalysis? = null
)

data class MangaAnalysisResult(
    val panels: List<PanelSegment>,
    val overallAnalysis: TextAnalysis? = null,
    val processingTime: Long = 0L
)