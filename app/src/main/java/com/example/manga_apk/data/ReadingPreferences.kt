package com.example.manga_apk.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

data class ReadingPreferences(
    val fontSize: TextUnit = 16.sp,
    val lineHeight: Float = 1.5f,
    val backgroundColor: ReadingTheme = ReadingTheme.LIGHT,
    val fontFamily: FontFamily = FontFamily.Default,
    val brightness: Float = 1.0f,
    val isNightMode: Boolean = false,
    val autoScroll: Boolean = false,
    val scrollSpeed: Float = 1.0f
)

enum class ReadingTheme(val backgroundColor: Color, val textColor: Color, val displayName: String) {
    LIGHT(Color(0xFFFFFFFF), Color(0xFF000000), "Light"),
    DARK(Color(0xFF121212), Color(0xFFE0E0E0), "Dark"),
    SEPIA(Color(0xFFF4F1E8), Color(0xFF5C4B37), "Sepia"),
    NIGHT(Color(0xFF000000), Color(0xFF00FF00), "Night")
}

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