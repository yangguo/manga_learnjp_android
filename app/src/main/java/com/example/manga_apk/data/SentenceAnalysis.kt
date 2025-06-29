package com.example.manga_apk.data

data class TextPosition(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

data class IdentifiedSentence(
    val id: Int,
    val text: String,
    val translation: String,
    val position: TextPosition,
    val vocabulary: List<VocabularyItem>,
    val grammarPatterns: List<GrammarPattern>
)

data class SentenceAnalysis(
    val originalSentence: String,
    val translation: String,
    val vocabulary: List<VocabularyItem>,
    val grammarPatterns: List<GrammarPattern> = emptyList(),
    val position: TextPosition? = null
)

data class TextAnalysis(
    val originalText: String,
    val translation: String,
    val vocabulary: List<VocabularyItem>,
    val grammarPatterns: List<GrammarPattern>,
    val context: String? = null,
    val sentenceAnalyses: List<SentenceAnalysis> = emptyList(),
    val identifiedSentences: List<IdentifiedSentence> = emptyList()
)