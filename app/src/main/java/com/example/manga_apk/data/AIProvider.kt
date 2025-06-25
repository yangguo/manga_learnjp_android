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
    val customConfig: CustomAPIConfig = CustomAPIConfig()
) {
    fun getConfiguredProviders(): List<AIProvider> {
        val providers = mutableListOf<AIProvider>()
        
        // Add primary provider first, but only if it's configured
        if (isProviderConfigured(primaryProvider)) {
            providers.add(primaryProvider)
        }
        
        // Add other configured providers if fallback is enabled
        if (enableFallback) {
            if (primaryProvider != AIProvider.OPENAI && isProviderConfigured(AIProvider.OPENAI)) {
                providers.add(AIProvider.OPENAI)
            }
            if (primaryProvider != AIProvider.GEMINI && isProviderConfigured(AIProvider.GEMINI)) {
                providers.add(AIProvider.GEMINI)
            }
            if (primaryProvider != AIProvider.CUSTOM && isProviderConfigured(AIProvider.CUSTOM)) {
                providers.add(AIProvider.CUSTOM)
            }
        }
        
        return providers
    }
    
    fun isProviderConfigured(provider: AIProvider): Boolean {
        return when (provider) {
            AIProvider.OPENAI -> openaiConfig.apiKey.trim().isNotEmpty()
            AIProvider.GEMINI -> geminiConfig.apiKey.trim().isNotEmpty()
            AIProvider.CUSTOM -> customConfig.apiKey.trim().isNotEmpty() && customConfig.endpoint.trim().isNotEmpty()
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



data class PanelSegment(
    val id: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val readingOrder: Int,
    val extractedText: String = "",
    val translation: String = "",
    val analysis: TextAnalysis? = null,
    val panelContext: String = "",
    val confidence: Float = 0.0f
)

data class PanelDetectionResult(
    val panels: List<DetectedPanel>,
    val readingOrder: List<Int>,
    val confidence: Float,
    val processingTime: Long = 0L
)

data class DetectedPanel(
    val id: String,
    val boundingBox: PanelBoundingBox,
    val readingOrder: Int,
    val confidence: Float,
    val panelType: PanelType = PanelType.DIALOGUE
)

data class PanelBoundingBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

enum class PanelType {
    DIALOGUE,
    NARRATION,
    SOUND_EFFECT,
    BACKGROUND_TEXT
}

data class MangaAnalysisResult(
    val panels: List<PanelSegment>,
    val overallAnalysis: TextAnalysis? = null,
    val processingTime: Long = 0L,
    val panelDetection: PanelDetectionResult? = null
)