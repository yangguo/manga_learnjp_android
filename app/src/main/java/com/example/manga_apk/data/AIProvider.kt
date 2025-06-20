package com.example.manga_apk.data

import android.graphics.Bitmap

enum class AIProvider(val displayName: String) {
    OPENAI("OpenAI GPT-4 Vision"),
    GEMINI("Google Gemini Vision"),
    CUSTOM("Custom OpenAI-Format API")
}

data class AIConfig(
    val provider: AIProvider = AIProvider.OPENAI,
    val apiKey: String = "",
    val customEndpoint: String = "",
    val customModel: String = "",
    val textModel: String = "gpt-4-turbo",
    val visionModel: String = "gpt-4o", // Updated to use the latest vision model
    val includeGrammar: Boolean = true,
    val includeVocabulary: Boolean = true,
    val includeTranslation: Boolean = true
)

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