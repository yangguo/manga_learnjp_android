package com.example.manga_apk.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import com.example.manga_apk.viewmodel.AnalysisMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.manga_apk.data.*
import com.example.manga_apk.viewmodel.MangaAnalysisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaAnalysisScreen(
    viewModel: MangaAnalysisViewModel,
    onNavigateToReading: () -> Unit,
    onNavigateToInteractiveReading: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStudyMode: () -> Unit = {},
    onNavigateToSpeedReading: () -> Unit = {},
    onNavigateToImmersiveMode: () -> Unit = {},
    onNavigateToVocabularyFocus: () -> Unit = {},
    onNavigateToDebugLog: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadImageFromUri(it) }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "Manga Learn JP",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(
                    onClick = onNavigateToDebugLog,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = "Debug Logs")
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onNavigateToSettings,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        
        when (uiState.currentMode) {
            AnalysisMode.UPLOAD -> {
                UploadSection(
                    selectedImage = uiState.selectedImage,
                    onImageSelect = { imagePickerLauncher.launch("image/*") },
                    isProcessing = uiState.isProcessing,
                    onQuickAnalysis = { 
                        viewModel.quickAnalysis()
                    },
                    onPanelAnalysis = { viewModel.setMode(AnalysisMode.PANEL_ANALYSIS) },
                    onReadingMode = { viewModel.setMode(AnalysisMode.READING_MODE) }
                )
            }
            AnalysisMode.PANEL_ANALYSIS -> {
                PanelAnalysisSection(
                    panels = uiState.panels,
                    selectedImage = uiState.selectedImage,
                    isProcessing = uiState.isProcessing,
                    onAnalyzePanel = viewModel::analyzePanel,
                    onBackToUpload = { viewModel.setMode(AnalysisMode.UPLOAD) }
                )
            }
            AnalysisMode.SIMPLE_ANALYSIS -> {
                SimpleAnalysisSection(
                    analysis = uiState.overallAnalysis,
                    selectedImage = uiState.selectedImage,
                    isProcessing = uiState.isProcessing,
                    onAnalyze = viewModel::analyzeWithFallback,
                    onBackToUpload = { viewModel.setMode(AnalysisMode.UPLOAD) },
                    onRunDemo = viewModel::runDemoAnalysis,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
            AnalysisMode.READING_MODE -> {
                ReadingModeSection(
                    onNavigateToReading = onNavigateToReading,
                    onNavigateToInteractiveReading = onNavigateToInteractiveReading,
                    onNavigateToStudyMode = onNavigateToStudyMode,
                    onNavigateToSpeedReading = onNavigateToSpeedReading,
                    onNavigateToImmersiveMode = onNavigateToImmersiveMode,
                    onNavigateToVocabularyFocus = onNavigateToVocabularyFocus
                )
            }
            AnalysisMode.STUDY_MODE -> {
                // Show study mode specific UI
                SimpleAnalysisSection(
                    analysis = uiState.overallAnalysis,
                    selectedImage = uiState.selectedImage,
                    isProcessing = uiState.isProcessing,
                    onAnalyze = viewModel::analyzeWithFallback,
                    onBackToUpload = { viewModel.setMode(AnalysisMode.UPLOAD) },
                    onRunDemo = viewModel::runDemoAnalysis,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
            AnalysisMode.SPEED_READING -> {
                // Show speed reading specific UI
                SimpleAnalysisSection(
                    analysis = uiState.overallAnalysis,
                    selectedImage = uiState.selectedImage,
                    isProcessing = uiState.isProcessing,
                    onAnalyze = viewModel::analyzeWithFallback,
                    onBackToUpload = { viewModel.setMode(AnalysisMode.UPLOAD) },
                    onRunDemo = viewModel::runDemoAnalysis,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
            AnalysisMode.IMMERSIVE_MODE -> {
                // Show immersive mode specific UI
                SimpleAnalysisSection(
                    analysis = uiState.overallAnalysis,
                    selectedImage = uiState.selectedImage,
                    isProcessing = uiState.isProcessing,
                    onAnalyze = viewModel::analyzeWithFallback,
                    onBackToUpload = { viewModel.setMode(AnalysisMode.UPLOAD) },
                    onRunDemo = viewModel::runDemoAnalysis,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
            AnalysisMode.VOCABULARY_FOCUS -> {
                // Show vocabulary focus specific UI
                SimpleAnalysisSection(
                    analysis = uiState.overallAnalysis,
                    selectedImage = uiState.selectedImage,
                    isProcessing = uiState.isProcessing,
                    onAnalyze = viewModel::analyzeWithFallback,
                    onBackToUpload = { viewModel.setMode(AnalysisMode.UPLOAD) },
                    onRunDemo = viewModel::runDemoAnalysis,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
        
        // Error handling - Enhanced with better visibility and specific API guidance
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Error Details:",
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Specific guidance for API errors
                    if (error.contains("404") || error.contains("Custom API")) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "🔧 Custom API Troubleshooting:",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Check that your API endpoint URL is correct\n" +
                                            "• Verify your API server is running and accessible\n" +
                                            "• For OpenAI-compatible APIs, try '/v1/chat/completions'\n" +
                                            "• For other providers, check their documentation\n" +
                                            "• Consider using OpenAI or Gemini as primary provider\n" +
                                            "• Enable fallback mode in AI Settings",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onNavigateToSettings,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("AI Settings", fontSize = 12.sp)
                                    }
                                    
                                    Button(
                                        onClick = { 
                                            viewModel.clearError()
                                            if (uiState.selectedImage != null) {
                                                viewModel.analyzeWithFallback()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = uiState.selectedImage != null && !uiState.isProcessing
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
                                
                                // Add demo button row
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { 
                                        viewModel.clearError()
                                        viewModel.runDemoAnalysis()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isProcessing,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Try Demo Analysis", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Check Android logs (Logcat) with tag 'MangaLearnJP' for detailed debugging information.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        // Debug information when processing
        if (uiState.isProcessing) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Processing...",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Check Logcat for detailed progress",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        
    }
}

@Composable
fun UploadSection(
    selectedImage: android.graphics.Bitmap?,
    onImageSelect: () -> Unit,
    isProcessing: Boolean,
    onQuickAnalysis: () -> Unit,
    onPanelAnalysis: () -> Unit,
    onReadingMode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Learn Japanese Through Manga",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Upload manga pages and let AI extract and analyze Japanese text\nwith detailed explanations",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Upload Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable { onImageSelect() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        ) {
            if (selectedImage != null) {
                Image(
                    bitmap = selectedImage.asImageBitmap(),
                    contentDescription = "Selected manga page",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Processing image...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Tap to upload manga image",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            "Supports PNG, JPG, WebP formats",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        if (selectedImage != null && !isProcessing) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Choose an analysis method:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Analysis buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Analysis Button
                Button(
                    onClick = {
                        println("UI: Quick AI Analysis button clicked")
                        android.util.Log.d("MangaLearnJP", "UI: Quick AI Analysis button clicked")
                        onQuickAnalysis()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quick AI Analysis")
                }
                
                // Panel Analysis Button
                OutlinedButton(
                    onClick = onPanelAnalysis,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.GridView,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Panel-by-Panel Analysis")
                }
                
                // Reading Mode Button
                OutlinedButton(
                    onClick = onReadingMode,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Interactive Reading Mode")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description text
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "• Quick Analysis: Get instant translation and vocabulary",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "• Panel Analysis: Break down manga into individual panels",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "• Reading Mode: Interactive study with word lookup",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ModeButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isPrimary) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = if (isPrimary) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PanelAnalysisSection(
    panels: List<PanelSegment>,
    selectedImage: android.graphics.Bitmap?,
    isProcessing: Boolean,
    onAnalyzePanel: (PanelSegment) -> Unit,
    onBackToUpload: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Panel Analysis",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            OutlinedButton(
                onClick = onBackToUpload
            ) {
                Icon(
                    Icons.Default.Upload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Image")
            }
        }
        
        if (selectedImage == null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Please upload an image first to analyze panels.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else if (panels.isEmpty() && !isProcessing) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "No panels detected. The image will be automatically segmented.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else if (isProcessing) {
            Card {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Segmenting panels...")
                }
            }
        } else {
            Text(
                text = "Detected ${panels.size} panels",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            panels.forEach { panel ->
                PanelCard(
                    panel = panel,
                    onAnalyze = { onAnalyzePanel(panel) }
                )
            }
        }
    }
}

@Composable
fun PanelCard(
    panel: PanelSegment,
    onAnalyze: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    "Panel ${panel.readingOrder}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Button(
                    onClick = onAnalyze,
                    enabled = panel.extractedText.isEmpty()
                ) {
                    Text(if (panel.extractedText.isEmpty()) "Analyze" else "Analyzed")
                }
            }
            
            if (panel.extractedText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Extracted Text:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    panel.extractedText,
                    fontSize = 14.sp
                )
                
                panel.analysis?.let { analysis ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Translation:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        analysis.translation,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleAnalysisSection(
    analysis: TextAnalysis?,
    selectedImage: android.graphics.Bitmap?,
    isProcessing: Boolean,
    onAnalyze: () -> Unit,
    onBackToUpload: () -> Unit,
    onRunDemo: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI Text Analysis",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            OutlinedButton(
                onClick = onBackToUpload
            ) {
                Icon(
                    Icons.Default.Upload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Image")
            }
        }
        
        if (selectedImage == null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Please upload an image first to perform analysis.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else if (isProcessing) {
            Card {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Analyzing image...")
                }
            }
        } else if (analysis == null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        println("UI: Analyze Full Image button clicked!")
                        println("UI: Image present: ${selectedImage != null}")
                        println("UI: Processing state: $isProcessing")
                        onAnalyze()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze Full Image")
                }
                
                // Demo button for when users want to see what the app can do
                OutlinedButton(
                    onClick = {
                        println("UI: Demo Analysis button clicked!")
                        onRunDemo()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Try Demo Analysis")
                }
                
                // Debug info card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "💡 AI Analysis Tips:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Configure your AI API key in Settings before analyzing\n" +
                                    "• If using Custom API, ensure the server is running\n" +
                                    "• Enable fallback mode for better reliability\n" +
                                    "• Test analysis will be shown if no API key is configured",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Configure AI Settings")
                        }
                    }
                }
            }
        } else {
            AnalysisResultCard(analysis = analysis)
            
            Button(
                onClick = onAnalyze,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Analyze Again")
            }
        }
    }
}

@Composable
fun ReadingModeSection(
    onNavigateToReading: () -> Unit,
    onNavigateToInteractiveReading: () -> Unit,
    onNavigateToStudyMode: () -> Unit = {},
    onNavigateToSpeedReading: () -> Unit = {},
    onNavigateToImmersiveMode: () -> Unit = {},
    onNavigateToVocabularyFocus: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Choose Your Reading Mode",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Select the reading mode that best fits your learning style and goals.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Interactive Reading Mode
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onNavigateToInteractiveReading() },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "📖 Interactive Reading",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tap on text for instant translation and analysis",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Study Mode
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onNavigateToStudyMode() },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "📚 Study Mode",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Highlight new words, show furigana, track progress",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Speed Reading Mode
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onNavigateToSpeedReading() },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "⚡ Speed Reading",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Paced reading with customizable WPM settings",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Immersive Mode
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onNavigateToImmersiveMode() },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "🎯 Immersive Mode",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Distraction-free full-screen reading experience",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Vocabulary Focus Mode
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onNavigateToVocabularyFocus() },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "📝 Vocabulary Focus",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Focus on JLPT level vocabulary with definitions",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onNavigateToReading,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Simple Reading Mode")
        }
    }
}

@Composable
fun AnalysisResultCard(analysis: TextAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Analysis Results",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (analysis.originalText.isNotEmpty()) {
                Text(
                    "Original Text:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    analysis.originalText,
                    fontSize = 14.sp
                )
            }
            
            Text(
                "Translation:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                analysis.translation,
                fontSize = 14.sp
            )
            
            if (analysis.vocabulary.isNotEmpty()) {
                Text(
                    "Vocabulary:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                analysis.vocabulary.take(5).forEach { vocab ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${vocab.word} (${vocab.reading})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            vocab.meaning,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}