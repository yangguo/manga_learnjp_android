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
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadImageFromUri(context, it) }
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
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
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
                    onBackToUpload = { viewModel.setMode(AnalysisMode.UPLOAD) }
                )
            }
            AnalysisMode.READING_MODE -> {
                ReadingModeSection(
                    onNavigateToReading = onNavigateToReading,
                    onNavigateToInteractiveReading = onNavigateToInteractiveReading
                )
            }
        }
        
        // Error handling
        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
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
                    onClick = onQuickAnalysis,
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
                        println("Analyze Full Image button clicked!")
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
                
                // Debug info card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "Make sure to configure your AI API key in Settings before analyzing. If no API key is set, a test analysis will be shown.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
            "This feature will allow you to tap on text in manga panels for instant translation and analysis.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onNavigateToReading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Reading Mode")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onNavigateToInteractiveReading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Interactive Reading")
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