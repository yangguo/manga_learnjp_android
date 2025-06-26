package com.example.manga_apk.ui

import android.util.Log
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
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
import com.example.manga_apk.utils.Logger
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.manga_apk.data.*
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveReadingScreen(
    selectedImage: android.graphics.Bitmap?,
    aiConfig: com.example.manga_apk.data.AIConfig,
    onShowSettings: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: com.example.manga_apk.viewmodel.InteractiveReadingViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.example.manga_apk.viewmodel.InteractiveReadingViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsState()
    
    // Set the AI config and image in the ViewModel when they change
    LaunchedEffect(selectedImage, aiConfig) {
        // Set AI config first, then the image
        viewModel.updateAIConfig(aiConfig)
        viewModel.setImage(selectedImage)
    }
    
    var selectedSentence by remember { mutableStateOf<IdentifiedSentence?>(null) }
    var showSentenceDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        modifier = modifier,
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
        if (selectedImage != null) {
            // Show interactive reading interface when image is available
            selectedImage.let { image ->
                EnhancedInteractiveView(
                    image = image,
                    identifiedSentences = uiState.identifiedSentences,
                    isAnalyzing = uiState.isAnalyzing,
                    onSentenceTap = { sentence ->
                        try {
                            selectedSentence = sentence
                            showSentenceDialog = true
                        } catch (e: Exception) {
                            Logger.logError("InteractiveReadingScreen", "Error handling sentence tap: ${e.message}")
                            // Optionally show a toast or error message to user
                        }
                    },
                    onRetryAnalysis = { viewModel.retryAnalysis() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        } else {
            // Show message when no image is available
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
                        text = "No image available",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Please go back and upload an image first",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    
    // Note: Word analysis functionality removed - focusing on sentence-level analysis
    
    // Error handling
    uiState.error?.let { error ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "Analysis Error:",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = viewModel::clearError) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onShowSettings,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Settings", fontSize = 12.sp)
                    }
                    
                    if (selectedImage != null) {
                        Button(
                            onClick = viewModel::retryAnalysis,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                contentColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry", fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "💡 Troubleshooting Tips:\n" +
                            "• Ensure image contains clear Japanese text\n" +
                            "• Try different AI providers in Settings\n" +
                            "• Check API key configuration\n" +
                            "• Use high-quality, well-lit manga images\n" +
                            "• Verify your internet connection",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Left
                )
            }
        }
    }
    
    // Sentence Analysis Dialog
    if (showSentenceDialog && selectedSentence != null) {
        SentenceAnalysisDialog(
            sentence = selectedSentence!!,
            onDismiss = { 
                showSentenceDialog = false
                selectedSentence = null  // Clear selection on dismiss
            }
        )
    }
}

@Composable
fun InteractiveVocabularyCard(
    vocabulary: VocabularyItem,
    onWordClick: (VocabularyItem) -> Unit = {}
) {
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = vocabulary.word,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (vocabulary.reading.isNotEmpty() && vocabulary.reading != vocabulary.word) {
                        Text(
                            text = vocabulary.reading,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                Text(
                    text = vocabulary.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
            if (vocabulary.partOfSpeech.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Part of speech: ${vocabulary.partOfSpeech}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun InteractiveGrammarPatternCard(pattern: GrammarPattern) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = pattern.pattern ?: "Unknown pattern",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (!pattern.explanation.isNullOrEmpty()) {
                Text(
                    text = pattern.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (!pattern.example.isNullOrEmpty() && pattern.example != pattern.pattern) {
                Text(
                    text = "Example: ${pattern.example}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}


@Composable
fun EnhancedInteractiveView(
    image: android.graphics.Bitmap,
    identifiedSentences: List<IdentifiedSentence>,
    isAnalyzing: Boolean,
    onSentenceTap: (IdentifiedSentence) -> Unit,
    onRetryAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    var imageSize by remember { mutableStateOf(Pair(0, 0)) }
    val density = LocalDensity.current
    
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
                        .clickable { 
                            try {
                                onSentenceTap(sentence)
                            } catch (e: Exception) {
                                Logger.logError("EnhancedInteractiveView", "Error in marker click: ${e.message}")
                            }
                        }
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
        
        // Bottom panel showing analysis status and identified sentences count
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isAnalyzing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Analyzing with LLM...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = "Identified Sentences (${identifiedSentences.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (identifiedSentences.isEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onRetryAnalysis,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry Analysis")
                        }
                    }
                }
            }
        }
    }

}

@Composable
fun SentenceAnalysisDialog(
    sentence: IdentifiedSentence,
    onDismiss: () -> Unit
) {
    // Validate content before rendering
    val hasText = sentence.text?.isNotEmpty() ?: false
    val hasTranslation = sentence.translation?.isNotEmpty() ?: false
    val hasVocabulary = sentence.vocabulary?.isNotEmpty() ?: false
    val hasGrammar = sentence.grammarPatterns?.isNotEmpty() ?: false
    
    if (!hasText && !hasTranslation) {
        // Show error dialog for completely empty content
        SimpleErrorDialog(
            title = "No Content Available",
            message = "This sentence contains no text or translation data.",
            onDismiss = onDismiss
        )
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)  // Responsive width
                .fillMaxHeight(0.9f)  // Prevent overflow on small screens
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Header with close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play audio")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
                
                HorizontalDivider()
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Japanese Text
                    if (hasText) {
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
                                    fontSize = 18.sp,
                                    lineHeight = 24.sp
                                )
                            }
                        }
                    }
                    
                    // Translation
                    if (hasTranslation) {
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
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                    
                    // Content quality indicator
                    if (!hasText || !hasTranslation || !hasVocabulary) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Some analysis data may be incomplete due to AI response limitations.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Vocabulary with height constraint
                    if (hasVocabulary) {
                        Text(
                            text = "Vocabulary (${sentence.vocabulary.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        // Use LazyColumn if many vocabulary items
                        if (sentence.vocabulary.size > 3) {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 200.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(sentence.vocabulary) { vocab ->
                                    InteractiveVocabularyCard(vocabulary = vocab, onWordClick = {})
                                }
                            }
                        } else {
                            sentence.vocabulary.forEach { vocab ->
                                InteractiveVocabularyCard(vocabulary = vocab, onWordClick = {})
                            }
                        }
                    } else {
                        // Show placeholder for missing vocabulary
                        Text(
                            text = "Vocabulary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = "No vocabulary data available for this sentence.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    
                    // Grammar Patterns with height constraint
                    if (hasGrammar) {
                        Text(
                            text = "Grammar Patterns (${sentence.grammarPatterns.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        if (sentence.grammarPatterns.size > 3) {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 200.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(sentence.grammarPatterns) { pattern ->
                                    InteractiveGrammarPatternCard(pattern = pattern)
                                }
                            }
                        } else {
                            sentence.grammarPatterns.forEach { pattern ->
                                InteractiveGrammarPatternCard(pattern = pattern)
                            }
                        }
                    } else {
                        Text(
                            text = "Grammar Patterns",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = "No grammar patterns identified for this sentence.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    
                    // Bottom padding for scrolling
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}





@Composable
fun SimpleErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

// Function to generate identified sentences directly from image using LLM analysis
// Note: This is now a placeholder that returns demo data.
// The actual LLM analysis should be performed in the ViewModel/Repository layer
// and passed to this screen as a parameter for better separation of concerns.
fun generateSentencesFromImage(image: android.graphics.Bitmap?): List<IdentifiedSentence> {
    if (image == null) return emptyList()
    
    // Return demo sentences for now
    // In a proper implementation, this data should come from:
    // 1. ViewModel calling AIService.analyzeImage() with interactive reading prompt
    // 2. Parsing the LLM response to extract sentences with positions
    // 3. Converting to IdentifiedSentence objects
    
    return listOf(
        IdentifiedSentence(
            id = 1,
            text = "もういい出かけてくる",
            translation = "Enough, I'm going out.",
            position = TextPosition(0.3f, 0.2f, 0.2f, 0.05f),
            vocabulary = listOf(
                VocabularyItem(
                    word = "出かける",
                    reading = "でかける",
                    meaning = "to go out",
                    partOfSpeech = "verb",
                    difficulty = 2
                )
            ),
            grammarPatterns = listOf(
                GrammarPattern(
                    pattern = "〜てくる",
                    explanation = "Te-form + kuru indicates going out and doing something",
                    example = "出かけてくる",
                    difficulty = "intermediate"
                )
            )
        ),
        IdentifiedSentence(
            id = 2,
            text = "留守番してろ！",
            translation = "Stay home and watch the house!",
            position = TextPosition(0.6f, 0.4f, 0.25f, 0.06f),
            vocabulary = listOf(
                VocabularyItem(
                    word = "留守番",
                    reading = "るすばん",
                    meaning = "house-sitting",
                    partOfSpeech = "noun",
                    difficulty = 3
                )
            ),
            grammarPatterns = listOf(
                GrammarPattern(
                    pattern = "〜してろ",
                    explanation = "Imperative form commanding someone to do something",
                    example = "留守番してろ",
                    difficulty = "intermediate"
                )
            )
        )
    )
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