package com.example.manga_apk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.manga_apk.data.*
import com.example.manga_apk.viewmodel.ReadingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyFocusScreen(
    viewModel: ReadingViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var selectedWord by remember { mutableStateOf<VocabularyItem?>(null) }
    
    // Sample vocabulary data
    val vocabularyItems = remember {
        listOf(
            VocabularyItem(
                word = "こんにちは",
                reading = "こんにちは",
                meaning = "Hello",
                partOfSpeech = "Greeting",
                jlptLevel = "N5",
                difficulty = 1
            ),
            VocabularyItem(
                word = "ありがとう",
                reading = "ありがとう", 
                meaning = "Thank you",
                partOfSpeech = "Expression",
                jlptLevel = "N5",
                difficulty = 1
            ),
            VocabularyItem(
                word = "学校",
                reading = "がっこう",
                meaning = "School",
                partOfSpeech = "Noun",
                jlptLevel = "N5",
                difficulty = 2
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("🎯 Vocabulary Focus") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showSettings = !showSettings }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )
        
        if (showSettings) {
            VocabularyFocusSettings(
                preferences = uiState.preferences,
                onUpdatePreferences = { viewModel.updatePreferences(it) },
                onDismiss = { showSettings = false }
            )
        }
        
        // Vocabulary List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(vocabularyItems) { vocab ->
                VocabularyCard(
                    vocabulary = vocab,
                    onWordClick = { selectedWord = it }
                )
            }
        }
    }
    
    // Word Detail Dialog
    selectedWord?.let { word ->
        AlertDialog(
            onDismissRequest = { selectedWord = null },
            title = { Text(word.word) },
            text = {
                Column {
                    Text("Reading: ${word.reading}")
                    Text("Meaning: ${word.meaning}")
                    Text("Part of Speech: ${word.partOfSpeech}")
                    word.jlptLevel?.let { Text("JLPT Level: $it") }
                    Text("Difficulty: ${word.difficulty}")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedWord = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun VocabularyCard(
    vocabulary: VocabularyItem,
    onWordClick: (VocabularyItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onWordClick(vocabulary) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${vocabulary.word} (${vocabulary.reading})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = vocabulary.meaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = vocabulary.difficulty.toString(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .background(
                        color = when (vocabulary.difficulty) {
                            1 -> Color(0xFF4CAF50)
                            2 -> Color(0xFFFF9800)
                            3 -> Color(0xFFF44336)
                            else -> Color.Gray
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VocabularyFocusSettings(
    preferences: ReadingPreferences,
    onUpdatePreferences: (ReadingPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Vocabulary Settings",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // JLPT Level Filter
            Text("JLPT Level Filter", style = MaterialTheme.typography.bodyMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("N5", "N4", "N3", "N2", "N1").forEach { level ->
                    FilterChip(
                        onClick = { /* Update filter */ },
                        label = { Text(level) },
                        selected = false
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Font Size
            Text("Font Size: ${preferences.fontSize}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = preferences.fontSize.toFloat(),
                onValueChange = { 
                    onUpdatePreferences(preferences.copy(fontSize = it.toInt()))
                },
                valueRange = 12f..24f,
                steps = 11
            )
        }
    }
}

@Composable
fun VocabularyListItem(
    item: VocabularyItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${item.word} (${item.reading})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.meaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = item.difficulty.toString(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .background(
                        color = when (item.difficulty) {
                            1 -> Color(0xFFE8F5E8)
                            2 -> Color(0xFFFFF3E0)
                            3 -> Color(0xFFFFEBEE)
                            else -> Color.Gray
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun VocabularyDetailDialog(
    vocabularyItem: VocabularyItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = vocabularyItem.word,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "Reading: ${vocabularyItem.reading}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Meaning: ${vocabularyItem.meaning}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Difficulty: ${when(vocabularyItem.difficulty) {
                        1 -> "Beginner"
                        2 -> "Elementary"
                        3 -> "Intermediate"
                        4 -> "Advanced"
                        5 -> "Expert"
                        else -> "Unknown"
                    }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = { /* Add to bookmarks */ }
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bookmark")
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
fun VocabularyFocusSettingsPanel(
    preferences: com.example.manga_apk.data.ReadingPreferences,
    onUpdatePreferences: (com.example.manga_apk.data.ReadingPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Vocabulary Focus Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Show furigana toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Furigana")
                Switch(
                    checked = preferences.vocabularyFocusSettings?.showFurigana ?: true,
                    onCheckedChange = { newValue ->
                        val newSettings = preferences.vocabularyFocusSettings?.copy(
                            showFurigana = newValue
                        ) ?: com.example.manga_apk.data.VocabularyFocusSettings(showFurigana = newValue)
                        onUpdatePreferences(preferences.copy(vocabularyFocusSettings = newSettings))
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Highlight new words toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Highlight New Words")
                Switch(
                    checked = preferences.vocabularyFocusSettings?.highlightNewWords ?: true,
                    onCheckedChange = { newValue ->
                        val newSettings = preferences.vocabularyFocusSettings?.copy(
                            highlightNewWords = newValue
                        ) ?: com.example.manga_apk.data.VocabularyFocusSettings(highlightNewWords = newValue)
                        onUpdatePreferences(preferences.copy(vocabularyFocusSettings = newSettings))
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Font size
            Text("Font Size: ${preferences.fontSize}sp")
            Slider(
                value = preferences.fontSize.toFloat(),
                onValueChange = { newValue ->
                    onUpdatePreferences(preferences.copy(fontSize = newValue.toInt()))
                },
                valueRange = 12f..24f,
                steps = 5
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}