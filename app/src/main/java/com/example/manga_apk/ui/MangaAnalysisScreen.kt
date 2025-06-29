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
import androidx.compose.ui.text.font.FontStyle
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
    onNavigateToInteractiveReading: () -> Unit,
    onNavigateToSettings: () -> Unit,
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
                    onReadingMode = { viewModel.setMode(AnalysisMode.READING_MODE) },
                    onNavigateToInteractiveReading = onNavigateToInteractiveReading
                )
            }
            AnalysisMode.PANEL_ANALYSIS -> {
                PanelAnalysisSection(
                    analysis = uiState.overallAnalysis?.originalText,
                    selectedImage = uiState.selectedImage,
                    isProcessing = uiState.isProcessing,
                    onAnalyze = viewModel::analyzeWithFallback,
                    onBackToUpload = { viewModel.setMode(AnalysisMode.UPLOAD) },
                    onRunDemo = viewModel::runDemoAnalysis
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
                    onNavigateToInteractiveReading = onNavigateToInteractiveReading
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
                    
                    // Specific guidance for different types of errors
                    if (error.contains("JSON parsing failed") || error.contains("Expected BEGIN_OBJECT but was STRING")) {
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
                                    text = "📝 JSON Parsing Issue:",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• The AI returned text instead of structured JSON data\n" +
                                            "• This is usually due to API configuration issues\n" +
                                            "• The app will automatically use fallback parsing\n" +
                                            "• Try using a different AI provider (OpenAI/Gemini)\n" +
                                            "• Check your API settings and prompts\n" +
                                            "• Enable enhanced fallback mode in settings",
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
                            }
                        }
                    } else if (error.contains("timeout") || error.contains("Network error: timeout") || error.contains("Request failed after")) {
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
                                    text = "⏱️ Network Timeout Issue:",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• The API request took too long to complete\n" +
                                            "• This is common with large images or slow connections\n" +
                                            "• The app now uses extended timeouts and retry logic\n" +
                                            "• Try reducing image size or using a different network\n" +
                                            "• Consider switching to a faster AI provider\n" +
                                            "• Check if your API server is responsive",
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
                                        Text("Settings", fontSize = 12.sp)
                                    }
                                    
                                    Button(
                                        onClick = { 
                                            viewModel.clearError()
                                            if (uiState.selectedImage != null) {
                                                viewModel.analyzeFullImage()
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
                            }
                        }
                    } else if (error.contains("404") || error.contains("Custom API")) {
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
    onReadingMode: () -> Unit,
    onNavigateToInteractiveReading: () -> Unit
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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                ) {
                    Icon(
                        Icons.Default.GridView,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Panel Analysis")
                }
                
                // Reading Mode Button
                OutlinedButton(
                    onClick = onNavigateToInteractiveReading,
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
                        "• Interactive Reading: Interactive study with word lookup",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PanelAnalysisSection(
    analysis: String?,
    selectedImage: android.graphics.Bitmap?,
    isProcessing: Boolean,
    onAnalyze: () -> Unit,
    onBackToUpload: () -> Unit,
    onRunDemo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            "Panel Analysis",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Image display
        if (selectedImage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Image(
                    bitmap = selectedImage.asImageBitmap(),
                    contentDescription = "Selected manga page",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Analysis controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAnalyze,
                enabled = !isProcessing && selectedImage != null,
                modifier = Modifier.weight(1f)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isProcessing) "Analyzing..." else "Analyze Panels")
            }
            
            OutlinedButton(
                onClick = onRunDemo,
                enabled = !isProcessing,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Demo")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Analysis results
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            if (analysis?.isNotEmpty() == true) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "Panel Analysis Results:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    item {
                        Text(
                            analysis,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.GridView,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Panel analysis will break down the manga page into individual panels and analyze each one separately.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Upload an image and click 'Analyze Panels' to get started.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Back button
        OutlinedButton(
            onClick = onBackToUpload,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back to Upload")
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
            
            // Show full analysis results when available
            panel.analysis?.let { analysis ->
                Spacer(modifier = Modifier.height(16.dp))
                AnalysisResultCard(analysis = analysis)
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                AnalysisResultCard(analysis = analysis)
            }
            
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
    onNavigateToInteractiveReading: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Interactive Reading Mode",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "This mode uses your uploaded manga image for interactive text analysis.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Interactive Reading Mode - Only mode that uses uploaded image
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Note about other reading modes
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "ℹ️ Other Reading Modes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Study Mode, Speed Reading, Immersive Mode, and Vocabulary Focus are available as standalone features that don't require uploaded images.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AnalysisResultCard(analysis: TextAnalysis) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Analysis Results",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Display sentence-by-sentence analysis if available
            if (analysis.sentenceAnalyses.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Sentence Analysis:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    analysis.sentenceAnalyses.forEachIndexed { index, sentence ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Sentence number
                                Text(
                                    "Sentence ${index + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                // Original Japanese text
                                if (sentence.originalSentence.isNotEmpty()) {
                                    Text(
                                        "Japanese: ${sentence.originalSentence}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                // Translation
                                Text(
                                    "Translation: ${sentence.translation}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                // Vocabulary for this sentence
                                 if (sentence.vocabulary.isNotEmpty()) {
                                     Text(
                                         "Key Vocabulary:",
                                         fontSize = 12.sp,
                                         fontWeight = FontWeight.Medium,
                                         color = MaterialTheme.colorScheme.secondary
                                     )
                                     sentence.vocabulary.take(5).forEach { vocab ->
                                         Text(
                                             "• ${vocab.word} (${vocab.reading}) - ${vocab.meaning}",
                                             fontSize = 11.sp,
                                             color = MaterialTheme.colorScheme.onSurfaceVariant,
                                             modifier = Modifier.padding(start = 8.dp)
                                         )
                                     }
                                 }
                                 
                                 // Grammar patterns for this sentence
                                val validGrammarPatterns = sentence.grammarPatterns.filter { 
                                    it.pattern.isNotEmpty() && it.pattern.isNotBlank() 
                                }
                                if (validGrammarPatterns.isNotEmpty()) {
                                    Text(
                                        "Grammar Patterns:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    validGrammarPatterns.take(3).forEach { grammar ->
                                         Column(
                                             modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                         ) {
                                             Text(
                                                 "• ${grammar.pattern}",
                                                 fontSize = 11.sp,
                                                 fontWeight = FontWeight.Medium,
                                                 color = MaterialTheme.colorScheme.onSurfaceVariant
                                             )
                                             if (grammar.explanation.isNotEmpty() && grammar.explanation.isNotBlank()) {
                                                 Text(
                                                     "  ${grammar.explanation}",
                                                     fontSize = 10.sp,
                                                     color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                     modifier = Modifier.padding(start = 4.dp)
                                                 )
                                             }
                                             if (grammar.example.isNotEmpty() && grammar.example.isNotBlank()) {
                                                 Text(
                                                     "  Example: ${grammar.example}",
                                                     fontSize = 10.sp,
                                                     fontStyle = FontStyle.Italic,
                                                     color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                     modifier = Modifier.padding(start = 4.dp)
                                                 )
                                             }
                                         }
                                     }
                                 }
                            }
                        }
                    }
                }
            } else {
                // Fallback to original display if no sentence analysis available
                if (analysis.originalText.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Original Text:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                analysis.originalText,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Translation:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            analysis.translation,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }


        }
    }
}