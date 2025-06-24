package com.example.manga_apk.data

data class SentenceAnalysis(
    val originalSentence: String,
    val translation: String,
    val vocabulary: List<VocabularyItem>
)

data class TextAnalysis(
    val originalText: String,
    val translation: String,
    val vocabulary: List<VocabularyItem>,
    val grammarPatterns: List<GrammarPattern>,
    val context: String? = null,
    val sentenceAnalyses: List<SentenceAnalysis> = emptyList()
)