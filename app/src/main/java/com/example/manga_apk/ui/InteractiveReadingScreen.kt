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
    
    // Set the image and AI config in the ViewModel when they change
    LaunchedEffect(selectedImage, aiConfig) {
        viewModel.setImage(selectedImage)
        viewModel.updateAIConfig(aiConfig)
    }
    
    var selectedSentence by remember { mutableStateOf<IdentifiedSentence?>(null) }
    var showSentenceDialog by remember { mutableStateOf(false) }
    
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
        if (selectedImage != null) {
            // Show interactive reading interface when image is available
            selectedImage?.let { image ->
                EnhancedInteractiveView(
                    image = image,
                    identifiedSentences = uiState.identifiedSentences,
                    isAnalyzing = uiState.isAnalyzing,
                    onSentenceTap = { sentence ->
                        selectedSentence = sentence
                        showSentenceDialog = true
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
            }
        }
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