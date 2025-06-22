package com.example.manga_apk.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import com.example.manga_apk.data.VocabularyItem
import com.example.manga_apk.data.GrammarPattern
import com.example.manga_apk.data.TextAnalysis
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.manga_apk.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveReadingScreen(
    panels: List<PanelSegment>,
    selectedImage: android.graphics.Bitmap?,
    onAnalyzeWord: (String) -> Unit,
    onShowSettings: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedPanel by remember { mutableStateOf<PanelSegment?>(null) }
    var selectedWord by remember { mutableStateOf<VocabularyItem?>(null) }
    var showWordDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Interactive Reading") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = onShowSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (panels.isEmpty()) {
            // Show message when no panels are available
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No panels detected",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Please go back and analyze the image first",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(panels) { panel ->
                    InteractivePanelCard(
                        panel = panel,
                        originalImage = selectedImage,
                        onWordTap = { word ->
                            selectedWord = word
                            showWordDialog = true
                        },
                        onPanelTap = {
                            selectedPanel = if (selectedPanel?.id == panel.id) null else panel
                        },
                        isExpanded = selectedPanel?.id == panel.id
                    )
                }
            }
        }
    }
    
    // Word Analysis Dialog
    if (showWordDialog && selectedWord != null) {
        WordAnalysisDialog(
            word = selectedWord!!,
            onDismiss = { showWordDialog = false }
        )
    }
}

@Composable
fun InteractivePanelCard(
    panel: PanelSegment,
    originalImage: android.graphics.Bitmap?,
    onWordTap: (VocabularyItem) -> Unit,
    onPanelTap: () -> Unit,
    isExpanded: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPanelTap() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Panel Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Panel ${panel.readingOrder}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (panel.extractedText?.isNotEmpty() == true) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Analyzed",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Panel Preview (if original image is available)
            originalImage?.let { bitmap ->
                val panelBitmap = remember(panel, bitmap) {
                    try {
                        android.graphics.Bitmap.createBitmap(
                            bitmap,
                            panel.x.coerceAtLeast(0),
                            panel.y.coerceAtLeast(0),
                            panel.width.coerceAtMost(bitmap.width - panel.x),
                            panel.height.coerceAtMost(bitmap.height - panel.y)
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (panelBitmap != null) {
                    AsyncImage(
                        model = panelBitmap,
                        contentDescription = "Panel ${panel.readingOrder}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Handle bitmap creation error
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Panel preview unavailable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Extracted Text
            if (panel.extractedText?.isNotEmpty() == true) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = panel.extractedText,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 18.sp
                    )
                }
            }
            
            // Expanded Analysis Content
            if (isExpanded && panel.analysis != null) {
                Divider()
                
                AnalysisContent(
                    analysis = panel.analysis,
                    onWordTap = onWordTap
                )
            }
        }
    }
}

@Composable
fun AnalysisContent(
    analysis: TextAnalysis,
    onWordTap: (VocabularyItem) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Translation
        if (analysis.translation.isNotEmpty()) {
            Column {
                Text(
                    text = "Translation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = analysis.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        // Vocabulary
        if (analysis.vocabulary.isNotEmpty()) {
            Column {
                Text(
                    text = "Vocabulary",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(analysis.vocabulary) { vocab ->
                        VocabularyCard(
                            vocabulary = vocab,
                            onClick = { onWordTap(vocab) }
                        )
                    }
                }
            }
        }
        
        // Grammar Patterns
        if (analysis.grammarPatterns.isNotEmpty()) {
            Column {
                Text(
                    text = "Grammar Patterns",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                analysis.grammarPatterns.forEach { pattern ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = pattern.pattern,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = pattern.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VocabularyCard(
    vocabulary: VocabularyItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = vocabulary.word,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (vocabulary.reading.isNotEmpty()) {
                    Text(
                        text = vocabulary.reading,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Text(
                text = vocabulary.meaning,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun WordAnalysisDialog(
    word: VocabularyItem,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Word Analysis",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Divider()
                
                // Word Details
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow("Word", word.word)
                    if (word.reading.isNotEmpty()) {
                        DetailRow("Reading", word.reading)
                    }
                    DetailRow("Meaning", word.meaning)
                    if (word.partOfSpeech.isNotEmpty()) {
                        DetailRow("Part of Speech", word.partOfSpeech)
                    }
                    if (word.difficulty > 0) {
                        DetailRow("Difficulty", word.difficulty.toString())
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}