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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import com.example.manga_apk.viewmodel.ReadingViewModel
import com.example.manga_apk.data.VocabularyItem

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
            VocabularyItem("図書館", "としょかん", "Library", "noun", "N4", 2),
            VocabularyItem("冒険", "ぼうけん", "Adventure", "noun", "N3", 3),
            VocabularyItem("秘密", "ひみつ", "Secret", "noun", "N4", 2),
            VocabularyItem("伝説", "でんせつ", "Legend", "noun", "N3", 3),
            VocabularyItem("古文書", "こぶんしょ", "Ancient document", "noun", "N1", 5),
            VocabularyItem("月明かり", "つきあかり", "Moonlight", "noun", "N3", 3),
            VocabularyItem("不思議", "ふしぎ", "Mysterious, strange", "adjective", "N4", 2)
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Vocabulary Focus") },
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

        // Vocabulary Stats
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VocabularyStatCard("Words Learned", "24", Color(0xFF4CAF50))
                VocabularyStatCard("New Words", "7", Color(0xFF2196F3))
                VocabularyStatCard("Difficulty", "N3", Color(0xFFFF9800))
            }
        }

        // Reading Content with Vocabulary Highlights
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Interactive Reading Text",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Interactive text with clickable vocabulary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    VocabularyHighlightedText(
                        text = "静かな夜の図書館で、古い本のページをめくる音だけが響いていた。主人公は長い間探していた古文書をついに見つけた。その本には不思議な力が宿っているという伝説があった。",
                        vocabularyItems = vocabularyItems,
                        onWordClick = { word -> selectedWord = word },
                        fontSize = uiState.preferences.fontSize
                    )
                }
            }
        }

        // Vocabulary List
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Vocabulary in this text",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyColumn {
                    items(vocabularyItems) { item ->
                        VocabularyListItem(
                            item = item,
                            onClick = { selectedWord = item }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Vocabulary Detail Dialog
    selectedWord?.let { word ->
        VocabularyDetailDialog(
            vocabularyItem = word,
            onDismiss = { selectedWord = null }
        )
    }

    // Settings Panel
    if (showSettings) {
        VocabularyFocusSettingsPanel(
            preferences = uiState.preferences,
            onUpdatePreferences = { viewModel.updatePreferences(it) },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
fun VocabularyStatCard(
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun VocabularyHighlightedText(
    text: String,
    vocabularyItems: List<VocabularyItem>,
    onWordClick: (VocabularyItem) -> Unit,
    fontSize: Int
) {
    // Simple implementation - in a real app, you'd want more sophisticated text parsing
    var processedText = text
    
    Column {
        vocabularyItems.forEach { vocab ->
            if (processedText.contains(vocab.word)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = vocab.word,
                        fontSize = fontSize.sp,
                        modifier = Modifier
                            .background(
                                color = when (vocab.difficulty) {
                                    "Beginner" -> Color(0xFFE8F5E8)
                                    "Intermediate" -> Color(0xFFFFF3E0)
                                    "Advanced" -> Color(0xFFFFEBEE)
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { onWordClick(vocab) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        color = when (vocab.difficulty) {
                            "Beginner" -> Color(0xFF2E7D32)
                            "Intermediate" -> Color(0xFFE65100)
                            "Advanced" -> Color(0xFFC62828)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = text,
            fontSize = fontSize.sp,
            lineHeight = (fontSize + 8).sp
        )
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
                text = item.difficulty,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .background(
                        color = when (item.difficulty) {
                            "Beginner" -> Color(0xFFE8F5E8)
                            "Intermediate" -> Color(0xFFFFF3E0)
                            "Advanced" -> Color(0xFFFFEBEE)
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