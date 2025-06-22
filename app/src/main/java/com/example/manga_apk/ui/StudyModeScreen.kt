package com.example.manga_apk.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.manga_apk.data.*
import com.example.manga_apk.viewmodel.ReadingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyModeScreen(
    viewModel: ReadingViewModel = viewModel()
) {
    val preferences by viewModel.readingPreferences
    val content by viewModel.readingContent
    val isSettingsVisible by viewModel.isSettingsVisible
    
    // Apply study mode when screen loads
    LaunchedEffect(Unit) {
        viewModel.setReadingMode(ReadingMode.STUDY)
    }
    
    val listState = rememberLazyListState()
    val studySettings = preferences.studyModeSettings
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(preferences.theme.backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Study Mode Top Bar
            TopAppBar(
                title = {
                    Text(
                        "📚 Study Mode",
                        color = preferences.theme.textColor
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleSettings() }
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = preferences.theme.textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = preferences.theme.backgroundColor
                )
            )
            
            // Study Progress Bar
            if (studySettings.autoSaveProgress) {
                LinearProgressIndicator(
                    progress = content.progressPercentage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Study Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StudyStatCard(
                    title = "New Words",
                    value = "12",
                    icon = "📝"
                )
                StudyStatCard(
                    title = "Reviewed",
                    value = "8",
                    icon = "✅"
                )
                StudyStatCard(
                    title = "Difficulty",
                    value = "N4",
                    icon = "📊"
                )
            }
            
            // Reading Content with Study Features
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .clickable { viewModel.toggleSettings() },
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    StudyModeText(
                        text = content.content,
                        preferences = preferences,
                        studySettings = studySettings,
                        onWordTap = { word ->
                            // Handle word tap for study
                        }
                    )
                }
            }
        }
        
        // Study Mode Settings Panel
        AnimatedVisibility(
            visible = isSettingsVisible,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            StudyModeSettingsPanel(
                preferences = preferences,
                studySettings = studySettings,
                onStudySettingsChange = { newSettings ->
                    viewModel.updateStudyModeSettings(newSettings)
                },
                onFontSizeChange = viewModel::updateFontSize,
                onLineHeightChange = viewModel::updateLineHeight,
                onThemeChange = viewModel::updateTheme,
                onClose = { viewModel.toggleSettings() }
            )
        }
    }
}

@Composable
fun StudyStatCard(
    title: String,
    value: String,
    icon: String
) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .height(60.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                fontSize = 16.sp
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StudyModeText(
    text: String,
    preferences: ReadingPreferences,
    studySettings: StudyModeSettings,
    onWordTap: (String) -> Unit
) {
    val annotatedText = buildAnnotatedString {
        val words = text.split(" ")
        
        words.forEachIndexed { index, word ->
            // Simulate highlighting new words (in real implementation, this would check against a vocabulary database)
            val isNewWord = word.length > 6 && studySettings.highlightNewWords
            val isDifficultWord = word.contains("漢字") || word.contains("日本語")
            
            if (isNewWord) {
                withStyle(
                    style = SpanStyle(
                        background = Color.Yellow.copy(alpha = 0.3f),
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(word)
                }
            } else if (isDifficultWord && studySettings.showDifficulty) {
                withStyle(
                    style = SpanStyle(
                        background = Color.Red.copy(alpha = 0.2f),
                        color = Color.Red.copy(alpha = 0.8f)
                    )
                ) {
                    append(word)
                }
            } else {
                withStyle(
                    style = SpanStyle(
                        color = preferences.theme.textColor
                    )
                ) {
                    append(word)
                }
            }
            
            if (index < words.size - 1) {
                append(" ")
            }
        }
    }
    
    Text(
        text = annotatedText,
        fontSize = preferences.fontSize.sp,
        lineHeight = preferences.lineHeight.sp,
        textAlign = TextAlign.Justify,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle text tap */ }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyModeSettingsPanel(
    preferences: ReadingPreferences,
    studySettings: StudyModeSettings,
    onStudySettingsChange: (StudyModeSettings) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onLineHeightChange: (Int) -> Unit,
    onThemeChange: (ReadingTheme) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Study Mode Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Study-specific settings
            Text(
                "Study Features",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Highlight New Words")
                Switch(
                    checked = studySettings.highlightNewWords,
                    onCheckedChange = { 
                        onStudySettingsChange(
                            studySettings.copy(highlightNewWords = it)
                        )
                    }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Furigana")
                Switch(
                    checked = studySettings.showFurigana,
                    onCheckedChange = { 
                        onStudySettingsChange(
                            studySettings.copy(showFurigana = it)
                        )
                    }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Word Tapping")
                Switch(
                    checked = studySettings.enableWordTapping,
                    onCheckedChange = { 
                        onStudySettingsChange(
                            studySettings.copy(enableWordTapping = it)
                        )
                    }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Difficulty")
                Switch(
                    checked = studySettings.showDifficulty,
                    onCheckedChange = { 
                        onStudySettingsChange(
                            studySettings.copy(showDifficulty = it)
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Font size slider
            Text(
                "Font Size: ${preferences.fontSize}sp",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = preferences.fontSize.toFloat(),
                onValueChange = { onFontSizeChange(it.toInt()) },
                valueRange = 12f..24f,
                steps = 11
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Line height slider
            Text(
                "Line Height: ${preferences.lineHeight}px",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = preferences.lineHeight.toFloat(),
                onValueChange = { onLineHeightChange(it.toInt()) },
                valueRange = 16f..32f,
                steps = 14
            )
        }
    }
}