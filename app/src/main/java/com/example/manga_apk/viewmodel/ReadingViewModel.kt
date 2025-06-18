package com.example.manga_apk.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.example.manga_apk.data.ReadingContent
import com.example.manga_apk.data.ReadingPreferences
import com.example.manga_apk.data.ReadingTheme

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
    
    companion object {
        private fun generateSampleContent(): String {
            return """
                Welcome to Reading Mode
                
                This is a sample reading application that demonstrates various reading features including:
                
                • Adjustable font sizes for comfortable reading
                • Multiple color themes (Light, Dark, Sepia, Night)
                • Line height adjustment for better readability
                • Auto-scroll functionality with speed control
                • Reading progress tracking
                • Brightness control
                • Night mode for low-light reading
                
                Reading Mode Features:
                
                Font Customization:
                You can adjust the font size from small to extra large to suit your reading preferences. The app supports different font families and allows you to customize line spacing for optimal readability.
                
                Theme Options:
                - Light Theme: Traditional white background with black text
                - Dark Theme: Dark background with light text for reduced eye strain
                - Sepia Theme: Warm, paper-like background that's easy on the eyes
                - Night Theme: High contrast green text on black background
                
                Auto-Scroll:
                Enable auto-scroll to automatically advance through the text at your preferred reading speed. You can adjust the scroll speed to match your reading pace.
                
                Progress Tracking:
                The app tracks your reading progress and shows how much of the content you've completed. This helps you keep track of your reading sessions.
                
                Brightness Control:
                Adjust the screen brightness directly from the reading interface without leaving the app. This is particularly useful for reading in different lighting conditions.
                
                Navigation:
                Use the settings panel to customize your reading experience. All settings are applied in real-time so you can see the changes immediately.
                
                This reading mode is designed to provide a distraction-free reading experience with all the customization options you need for comfortable reading sessions.
                
                Enjoy your reading!
            """.trimIndent()
        }
    }
}