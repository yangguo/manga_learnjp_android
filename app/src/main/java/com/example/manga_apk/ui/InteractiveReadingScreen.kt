package com.example.manga_apk.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import com.example.manga_apk.data.VocabularyItem
import com.example.manga_apk.data.GrammarPattern
import com.example.manga_apk.data.TextAnalysis
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.manga_apk.data.*
import kotlin.math.roundToInt

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
    var selectedSentence by remember { mutableStateOf<IdentifiedSentence?>(null) }
    var showSentenceDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("enhanced") } // "enhanced" or "classic"
    
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
                    // Toggle view mode
                    TextButton(
                        onClick = { 
                            viewMode = if (viewMode == "enhanced") "classic" else "enhanced"
                        }
                    ) {
                        Text(if (viewMode == "enhanced") "Classic" else "Enhanced")
                    }
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
            if (viewMode == "enhanced" && selectedImage != null) {
                // Enhanced mode with image overlay and markers
                EnhancedInteractiveView(
                    image = selectedImage,
                    panels = panels,
                    onSentenceTap = { sentence ->
                        selectedSentence = sentence
                        showSentenceDialog = true
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            } else {
                // Classic mode with panel cards
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
    }
    
    // Word Analysis Dialog
    if (showWordDialog && selectedWord != null) {
        WordAnalysisDialog(
            word = selectedWord!!,
            onDismiss = { showWordDialog = false }
        )
    }
    
    // Sentence Analysis Dialog
    if (showSentenceDialog && selectedSentence != null) {
        SentenceAnalysisDialog(
            sentence = selectedSentence!!,
            onDismiss = { showSentenceDialog = false }
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
fun EnhancedInteractiveView(
    image: android.graphics.Bitmap,
    panels: List<PanelSegment>,
    onSentenceTap: (IdentifiedSentence) -> Unit,
    modifier: Modifier = Modifier
) {
    var imageSize by remember { mutableStateOf(Pair(0, 0)) }
    val density = LocalDensity.current
    
    // Generate mock identified sentences for demonstration
    val identifiedSentences = remember(panels) {
        generateMockIdentifiedSentences(panels)
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Manga image
        AsyncImage(
            model = image,
            contentDescription = "Manga page",
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    imageSize = Pair(coordinates.size.width, coordinates.size.height)
                },
            contentScale = ContentScale.Fit
        )
        
        // Text markers overlay
        if (imageSize.first > 0 && imageSize.second > 0) {
            identifiedSentences.forEach { sentence ->
                val markerX = (sentence.position.x * imageSize.first).roundToInt()
                val markerY = (sentence.position.y * imageSize.second).roundToInt()
                
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { markerX.toDp() },
                            y = with(density) { markerY.toDp() }
                        )
                        .size(32.dp)
                        .background(
                            Color.Red,
                            CircleShape
                        )
                        .border(
                            2.dp,
                            Color.White,
                            CircleShape
                        )
                        .clickable { onSentenceTap(sentence) }
                        .zIndex(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sentence.id.toString(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Bottom panel showing identified sentences count
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            )
        ) {
            Text(
                text = "Identified Sentences (${identifiedSentences.size})",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SentenceAnalysisDialog(
    sentence: IdentifiedSentence,
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
                        text = "Sentence Analysis",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(onClick = { /* TODO: Add TTS */ }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Play audio")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
                
                Divider()
                
                // Japanese Text
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Japanese Text",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sentence.text,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 18.sp
                        )
                    }
                }
                
                // Translation
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Translation",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sentence.translation,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // Vocabulary
                if (sentence.vocabulary.isNotEmpty()) {
                    Text(
                        text = "Vocabulary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    sentence.vocabulary.forEach { vocab ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = vocab.word,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (vocab.reading.isNotEmpty()) {
                                            Text(
                                                text = vocab.reading,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                    Text(
                                        text = vocab.meaning,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (vocab.partOfSpeech.isNotEmpty()) {
                                    Text(
                                        text = "Part of speech: ${vocab.partOfSpeech}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Grammar Patterns
                if (sentence.grammarPatterns.isNotEmpty()) {
                    Text(
                        text = "Grammar Patterns",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    sentence.grammarPatterns.forEach { pattern ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper function to generate mock identified sentences for demonstration
fun generateMockIdentifiedSentences(panels: List<PanelSegment>): List<IdentifiedSentence> {
    val sentences = mutableListOf<IdentifiedSentence>()
    var sentenceId = 1
    
    panels.forEach { panel ->
        if (panel.extractedText.isNotEmpty()) {
            // Create mock sentences based on panel text
            val mockSentences = listOf(
                IdentifiedSentence(
                    id = sentenceId++,
                    text = "もういい出かけてくる",
                    translation = "Enough, I'm going out.",
                    position = TextPosition(0.3f, 0.2f, 0.2f, 0.05f),
                    vocabulary = listOf(
                        VocabularyItem(
                            word = "出かける",
                            reading = "でかける",
                            meaning = "to go out",
                            partOfSpeech = "verb (imperative)",
                            difficulty = 2
                        )
                    ),
                    grammarPatterns = listOf(
                        GrammarPattern(
                            pattern = "〜てくる",
                            explanation = "Imperative form of する, commanding to do something.",
                            example = "出かけてくる",
                            difficulty = "intermediate"
                        )
                    )
                ),
                IdentifiedSentence(
                    id = sentenceId++,
                    text = "留守番してろ！",
                    translation = "Stay home and watch the house!",
                    position = TextPosition(0.6f, 0.4f, 0.25f, 0.06f),
                    vocabulary = listOf(
                        VocabularyItem(
                            word = "留守番",
                            reading = "るすばん",
                            meaning = "house-sitting/staying home",
                            partOfSpeech = "noun",
                            difficulty = 3
                        ),
                        VocabularyItem(
                            word = "してろ",
                            reading = "してろ",
                            meaning = "do (imperative form of する)",
                            partOfSpeech = "verb (imperative)",
                            difficulty = 2
                        )
                    ),
                    grammarPatterns = listOf(
                        GrammarPattern(
                            pattern = "〜してろ",
                            explanation = "Imperative form of する, commanding to do something.",
                            example = "留守番してろ",
                            difficulty = "intermediate"
                        )
                    )
                )
            )
            sentences.addAll(mockSentences.take(2)) // Limit to 2 sentences per panel for demo
        }
    }
    
    return sentences
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