package com.example.manga_apk.data

// PanelDetectionResult, DetectedPanel, PanelBoundingBox, and PanelType are already defined in AIProvider.kt

/**
 * Represents analysis of text within a specific panel
 */
data class PanelTextAnalysis(
    val panelId: String,
    val extractedText: String,
    val translation: String,
    val vocabulary: List<VocabularyItem>,
    val grammarPatterns: List<GrammarPattern>,
    val context: String,
    val confidence: Float
)

// VocabularyItem is already defined in AIProvider.kt

// GrammarPattern is already defined in AIProvider.kt

/**
 * Complete analysis result for a manga page with panels
 */
data class MangaPageAnalysis(
    val panelDetection: PanelDetectionResult,
    val panelAnalyses: List<PanelTextAnalysis>,
    val overallContext: String,
    val processingTime: Long
)