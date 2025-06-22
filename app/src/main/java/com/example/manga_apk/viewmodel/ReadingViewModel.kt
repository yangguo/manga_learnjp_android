package com.example.manga_apk.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.ViewModel
import com.example.manga_apk.data.*

class ReadingViewModel : ViewModel() {
    
    private val _readingPreferences = mutableStateOf(ReadingPreferences())
    val readingPreferences: State<ReadingPreferences> = _readingPreferences
    
    private val _readingContent = mutableStateOf(
        ReadingContent(
            title = "Sample Reading Content",
            content = generateSampleContent()
        )
    )
    val readingContent: State<ReadingContent> = _readingContent
    
    private val _isSettingsVisible = mutableStateOf(false)
    val isSettingsVisible: State<Boolean> = _isSettingsVisible
    
    private val _isAutoScrolling = mutableStateOf(false)
    val isAutoScrolling: State<Boolean> = _isAutoScrolling
    
    fun updateFontSize(size: TextUnit) {
        _readingPreferences.value = _readingPreferences.value.copy(fontSize = size)
    }
    
    fun updateLineHeight(height: Float) {
        _readingPreferences.value = _readingPreferences.value.copy(lineHeight = height)
    }
    
    fun updateTheme(theme: ReadingTheme) {
        _readingPreferences.value = _readingPreferences.value.copy(backgroundColor = theme)
    }
    
    fun updateFontFamily(fontFamily: FontFamily) {
        _readingPreferences.value = _readingPreferences.value.copy(fontFamily = fontFamily)
    }
    
    fun updateBrightness(brightness: Float) {
        _readingPreferences.value = _readingPreferences.value.copy(brightness = brightness)
    }
    
    fun toggleNightMode() {
        _readingPreferences.value = _readingPreferences.value.copy(
            isNightMode = !_readingPreferences.value.isNightMode
        )
    }
    
    fun toggleAutoScroll() {
        _isAutoScrolling.value = !_isAutoScrolling.value
        _readingPreferences.value = _readingPreferences.value.copy(
            autoScroll = _isAutoScrolling.value
        )
    }
    
    fun updateScrollSpeed(speed: Float) {
        _readingPreferences.value = _readingPreferences.value.copy(scrollSpeed = speed)
    }
    
    fun updateReadingPosition(position: Int) {
        _readingContent.value = _readingContent.value.copy(currentPosition = position)
    }
    
    fun toggleSettingsVisibility() {
        _isSettingsVisible.value = !_isSettingsVisible.value
    }
    
    fun loadContent(title: String, content: String) {
        _readingContent.value = ReadingContent(title = title, content = content)
    }
    
    // Reading Mode Functions
    fun setReadingMode(mode: ReadingMode) {
        _readingPreferences.value = _readingPreferences.value.copy(readingMode = mode)
        
        // Apply mode-specific settings
        when (mode) {
            ReadingMode.STUDY -> applyStudyModeDefaults()
            ReadingMode.SPEED_READING -> applySpeedReadingDefaults()
            ReadingMode.IMMERSIVE -> applyImmersiveModeDefaults()
            ReadingMode.VOCABULARY_FOCUS -> applyVocabularyFocusDefaults()
            ReadingMode.NORMAL -> applyNormalModeDefaults()
        }
    }
    
    private fun applyStudyModeDefaults() {
        _readingPreferences.value = _readingPreferences.value.copy(
            backgroundColor = ReadingTheme.STUDY,
            fontSize = 18.sp,
            lineHeight = 1.8f,
            studyModeSettings = StudyModeSettings(
                highlightNewWords = true,
                showFurigana = true,
                enableWordTapping = true,
                autoSaveProgress = true,
                showDifficulty = true
            )
        )
    }
    
    private fun applySpeedReadingDefaults() {
        _readingPreferences.value = _readingPreferences.value.copy(
            backgroundColor = ReadingTheme.FOCUS,
            fontSize = 16.sp,
            lineHeight = 1.4f,
            speedReadingSettings = SpeedReadingSettings(
                wordsPerMinute = 200,
                enablePacing = true,
                highlightCurrentWord = true,
                pauseOnDifficultWords = false,
                showProgressIndicator = true
            )
        )
    }
    
    private fun applyImmersiveModeDefaults() {
        _readingPreferences.value = _readingPreferences.value.copy(
            backgroundColor = ReadingTheme.DARK,
            fontSize = 17.sp,
            lineHeight = 1.6f,
            immersiveModeSettings = ImmersiveModeSettings(
                hideUI = true,
                fullScreenMode = true,
                minimizeDistractions = true,
                enableGestureNavigation = true,
                autoHideControls = true
            )
        )
    }
    
    private fun applyVocabularyFocusDefaults() {
        _readingPreferences.value = _readingPreferences.value.copy(
            backgroundColor = ReadingTheme.LIGHT,
            fontSize = 18.sp,
            lineHeight = 1.7f,
            vocabularyFocusSettings = VocabularyFocusSettings(
                highlightLevel = "N5",
                showDefinitions = true,
                enableQuizMode = false,
                trackLearningProgress = true,
                prioritizeUnknownWords = true
            )
        )
    }
    
    private fun applyNormalModeDefaults() {
        _readingPreferences.value = _readingPreferences.value.copy(
            backgroundColor = ReadingTheme.LIGHT,
            fontSize = 16.sp,
            lineHeight = 1.5f
        )
    }
    
    // Study Mode specific functions
    fun updateStudyModeSettings(settings: StudyModeSettings) {
        _readingPreferences.value = _readingPreferences.value.copy(
            studyModeSettings = settings
        )
    }
    
    // Speed Reading specific functions
    fun updateSpeedReadingSettings(settings: SpeedReadingSettings) {
        _readingPreferences.value = _readingPreferences.value.copy(
            speedReadingSettings = settings
        )
    }
    
    // Immersive Mode specific functions
    fun updateImmersiveModeSettings(settings: ImmersiveModeSettings) {
        _readingPreferences.value = _readingPreferences.value.copy(
            immersiveModeSettings = settings
        )
    }
    
    // Vocabulary Focus specific functions
    fun updateVocabularyFocusSettings(settings: VocabularyFocusSettings) {
        _readingPreferences.value = _readingPreferences.value.copy(
            vocabularyFocusSettings = settings
        )
    }
    
    companion object {
        private fun generateSampleContent(): String {
            return """
                Welcome to Enhanced Reading Mode
                
                This is a sample reading content to demonstrate the new reading interface with multiple specialized modes. You can customize various aspects of the reading experience using the settings panel.
                
                Available Reading Modes:
                
                📖 Interactive Reading Mode:
                • Tap on text for instant translation
                • Real-time word analysis
                • Panel-based manga reading
                
                📚 Study Mode:
                • Highlight new vocabulary words
                • Show furigana for kanji
                • Track learning progress
                • Auto-save reading position
                • Display word difficulty levels
                
                ⚡ Speed Reading Mode:
                • Customizable words per minute (WPM)
                • Paced reading with highlighting
                • Progress indicators
                • Pause on difficult words option
                
                🎯 Immersive Mode:
                • Full-screen distraction-free reading
                • Auto-hide UI controls
                • Gesture-based navigation
                • Minimized visual distractions
                
                📝 Vocabulary Focus Mode:
                • JLPT level-based highlighting (N5-N1)
                • Instant definitions display
                • Quiz mode for vocabulary practice
                • Learning progress tracking
                • Prioritize unknown words
                
                General Features:
                • Adjustable font size (12sp to 24sp)
                • Multiple themes (Light, Dark, Sepia, Night, Focus, Study)
                • Line height customization
                • Font family selection
                • Brightness control
                • Auto-scroll functionality
                
                Each reading mode is optimized for different learning goals and reading preferences. Switch between modes to find what works best for your Japanese learning journey.
                
                この機能は日本語学習者のために特別に設計されています。漫画を読みながら新しい単語を学び、文法を理解し、読解力を向上させることができます。
            """.trimIndent()
        }
    }
}