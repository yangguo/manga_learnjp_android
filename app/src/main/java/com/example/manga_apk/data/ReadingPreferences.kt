package com.example.manga_apk.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

data class ReadingPreferences(
    val fontSize: Int = 16,
    val lineHeight: Int = 24,
    val theme: ReadingTheme = ReadingTheme.LIGHT,
    val fontFamily: FontFamily = FontFamily.Default,
    val brightness: Float = 1.0f,
    val isNightMode: Boolean = false,
    val autoScroll: Boolean = false,
    val scrollSpeed: Float = 1.0f,
    val readingMode: ReadingMode = ReadingMode.NORMAL,
    val studyModeSettings: StudyModeSettings = StudyModeSettings(),
    val speedReadingSettings: SpeedReadingSettings = SpeedReadingSettings(),
    val immersiveModeSettings: ImmersiveModeSettings = ImmersiveModeSettings(),
    val vocabularyFocusSettings: VocabularyFocusSettings = VocabularyFocusSettings()
)

enum class ReadingTheme(val backgroundColor: Color, val textColor: Color, val displayName: String) {
    LIGHT(Color(0xFFFFFFFF), Color(0xFF000000), "Light"),
    DARK(Color(0xFF121212), Color(0xFFE0E0E0), "Dark"),
    SEPIA(Color(0xFFF4F1E8), Color(0xFF5C4B37), "Sepia"),
    NIGHT(Color(0xFF000000), Color(0xFF00FF00), "Night"),
    FOCUS(Color(0xFFF8F8F8), Color(0xFF2C2C2C), "Focus"),
    STUDY(Color(0xFFFFFDE7), Color(0xFF3E2723), "Study")
}

enum class ReadingMode {
    NORMAL,
    STUDY,
    SPEED_READING,
    IMMERSIVE,
    VOCABULARY_FOCUS
}

data class StudyModeSettings(
    val highlightNewWords: Boolean = true,
    val showFurigana: Boolean = true,
    val enableWordTapping: Boolean = true,
    val autoSaveProgress: Boolean = true,
    val showDifficulty: Boolean = true
)

data class SpeedReadingSettings(
    val wordsPerMinute: Int = 200,
    val enablePacing: Boolean = true,
    val highlightCurrentWord: Boolean = true,
    val pauseOnDifficultWords: Boolean = false,
    val showProgressIndicator: Boolean = true
)

data class ImmersiveModeSettings(
    val hideUI: Boolean = true,
    val fullScreenMode: Boolean = true,
    val minimizeDistractions: Boolean = true,
    val enableGestureNavigation: Boolean = true,
    val autoHideControls: Boolean = true
)

data class VocabularyFocusSettings(
    val highlightLevel: String = "N5", // JLPT levels
    val showDefinitions: Boolean = true,
    val enableQuizMode: Boolean = false,
    val trackLearningProgress: Boolean = true,
    val prioritizeUnknownWords: Boolean = true,
    val showFurigana: Boolean = true,
    val highlightNewWords: Boolean = true
)

data class ReadingContent(
    val title: String,
    val content: String,
    val currentPosition: Int = 0
) {
    val totalLength: Int
        get() = content.length
        
    val progressPercentage: Float
        get() = if (totalLength > 0) currentPosition.toFloat() / totalLength else 0f
}