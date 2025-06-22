package com.example.manga_apk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.manga_apk.data.ReadingPreferences
import com.example.manga_apk.data.ReadingTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsPanel(
    preferences: ReadingPreferences,
    isAutoScrolling: Boolean,
    onFontSizeChange: (androidx.compose.ui.unit.TextUnit) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onThemeChange: (ReadingTheme) -> Unit,
    onFontFamilyChange: (FontFamily) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onNightModeToggle: () -> Unit,
    onAutoScrollToggle: () -> Unit,
    onScrollSpeedChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (preferences.isNightMode) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Reading Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (preferences.isNightMode) Color.White else Color.Black
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (preferences.isNightMode) Color.White else Color.Black
                        )
                    }
                }
                Divider(color = if (preferences.isNightMode) Color.Gray else Color.LightGray)
            }
            
            item {
                // Font Size
                SettingSection(
                    title = "Font Size",
                    isNightMode = preferences.isNightMode
                ) {
                    val fontSizes = listOf(12.sp, 14.sp, 16.sp, 18.sp, 20.sp, 24.sp, 28.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        fontSizes.forEach { size ->
                            FilterChip(
                                onClick = { onFontSizeChange(size) },
                                label = { Text("${size.value.toInt()}") },
                                selected = preferences.fontSize == size,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
            
            item {
                // Line Height
                SettingSection(
                    title = "Line Height",
                    isNightMode = preferences.isNightMode
                ) {
                    Slider(
                        value = preferences.lineHeight,
                        onValueChange = onLineHeightChange,
                        valueRange = 1.0f..2.5f,
                        steps = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${String.format("%.1f", preferences.lineHeight)}x",
                        fontSize = 12.sp,
                        color = if (preferences.isNightMode) Color.Gray else Color.DarkGray
                    )
                }
            }
            
            item {
                // Themes
                SettingSection(
                    title = "Theme",
                    isNightMode = preferences.isNightMode
                ) {
                    Column {
                        ReadingTheme.entries.forEach { theme ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onThemeChange(theme) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = preferences.backgroundColor == theme,
                                    onClick = { onThemeChange(theme) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(theme.backgroundColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    theme.displayName,
                                    color = if (preferences.isNightMode) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                // Font Family
                SettingSection(
                    title = "Font Family",
                    isNightMode = preferences.isNightMode
                ) {
                    val fontFamilies = listOf(
                        FontFamily.Default to "Default",
                        FontFamily.Serif to "Serif",
                        FontFamily.SansSerif to "Sans Serif",
                        FontFamily.Monospace to "Monospace"
                    )
                    Column {
                        fontFamilies.forEach { (family, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFontFamilyChange(family) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = preferences.fontFamily == family,
                                    onClick = { onFontFamilyChange(family) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    name,
                                    fontFamily = family,
                                    color = if (preferences.isNightMode) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                // Brightness
                SettingSection(
                    title = "Brightness",
                    isNightMode = preferences.isNightMode
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Brightness6,
                            contentDescription = null,
                            tint = if (preferences.isNightMode) Color.White else Color.Black
                        )
                        Slider(
                            value = preferences.brightness,
                            onValueChange = onBrightnessChange,
                            valueRange = 0.3f..1.0f,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Text(
                            "${(preferences.brightness * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = if (preferences.isNightMode) Color.Gray else Color.DarkGray
                        )
                    }
                }
            }
            
            item {
                // Auto Scroll
                SettingSection(
                    title = "Auto Scroll",
                    isNightMode = preferences.isNightMode
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Enable Auto Scroll",
                            color = if (preferences.isNightMode) Color.White else Color.Black
                        )
                        Switch(
                            checked = isAutoScrolling,
                            onCheckedChange = { onAutoScrollToggle() }
                        )
                    }
                    
                    if (isAutoScrolling) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Scroll Speed",
                            fontSize = 14.sp,
                            color = if (preferences.isNightMode) Color.White else Color.Black
                        )
                        Slider(
                            value = preferences.scrollSpeed,
                            onValueChange = onScrollSpeedChange,
                            valueRange = 0.5f..3.0f,
                            steps = 10,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "${String.format("%.1f", preferences.scrollSpeed)}x",
                            fontSize = 12.sp,
                            color = if (preferences.isNightMode) Color.Gray else Color.DarkGray
                        )
                    }
                }
            }
            
            item {
                // Night Mode Toggle
                SettingSection(
                    title = "Display",
                    isNightMode = preferences.isNightMode
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (preferences.isNightMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = if (preferences.isNightMode) Color.White else Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Night Mode",
                                color = if (preferences.isNightMode) Color.White else Color.Black
                            )
                        }
                        Switch(
                            checked = preferences.isNightMode,
                            onCheckedChange = { onNightModeToggle() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingSection(
    title: String,
    isNightMode: Boolean,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (isNightMode) Color.White else Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}