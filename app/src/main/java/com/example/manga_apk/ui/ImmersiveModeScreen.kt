package com.example.manga_apk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.manga_apk.viewmodel.ReadingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveModeScreen(
    viewModel: ReadingViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                when (uiState.preferences.theme) {
                    com.example.manga_apk.data.ReadingTheme.DARK -> Color.Black
                    com.example.manga_apk.data.ReadingTheme.NIGHT -> Color(0xFF0D1117)
                    com.example.manga_apk.data.ReadingTheme.SEPIA -> Color(0xFFF4F1EA)
                    else -> MaterialTheme.colorScheme.background
                }
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar (hidden in fullscreen)
            if (!isFullscreen) {
                TopAppBar(
                    title = { Text("Immersive Reading") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isFullscreen = !isFullscreen }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen")
                        }
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }

            // Immersive Reading Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isFullscreen) 0.dp else 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = if (isFullscreen) 32.dp else 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        text = uiState.content?.title ?: "Immersive Reading Experience",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color =                        when (uiState.preferences.theme) {
                            com.example.manga_apk.data.ReadingTheme.DARK, 
                            com.example.manga_apk.data.ReadingTheme.NIGHT -> Color.White
                            com.example.manga_apk.data.ReadingTheme.SEPIA -> Color(0xFF5D4E37)
                            else -> MaterialTheme.colorScheme.onBackground
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Reading Content with enhanced typography
                    Text(
                        text = uiState.content?.content ?: generateImmersiveContent(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = (uiState.preferences.fontSize + 4).sp,
                        lineHeight = (uiState.preferences.lineHeight + 8).sp,
                        textAlign = TextAlign.Justify,
                        color =                        when (uiState.preferences.theme) {
                            com.example.manga_apk.data.ReadingTheme.DARK, 
                            com.example.manga_apk.data.ReadingTheme.NIGHT -> Color.White.copy(alpha = 0.9f)
                            com.example.manga_apk.data.ReadingTheme.SEPIA -> Color(0xFF5D4E37).copy(alpha = 0.8f)
                            else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        },
                        modifier = Modifier.padding(horizontal = if (isFullscreen) 32.dp else 16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // Reading Progress
                    if (!isFullscreen) {
                        LinearProgressIndicator(
                            progress = (uiState.content?.progressPercentage ?: 0f) / 100f,
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "${((uiState.content?.progressPercentage ?: 0f) * 100).toInt()}% Complete",
                            style = MaterialTheme.typography.bodySmall,
                            color =                            when (uiState.preferences.theme) {
                                com.example.manga_apk.data.ReadingTheme.DARK, 
                                com.example.manga_apk.data.ReadingTheme.NIGHT -> Color.White.copy(alpha = 0.6f)
                                com.example.manga_apk.data.ReadingTheme.SEPIA -> Color(0xFF5D4E37).copy(alpha = 0.6f)
                                else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            }
                        )
                    }
                }
                
                // Fullscreen toggle button (always visible)
                if (isFullscreen) {
                    FloatingActionButton(
                        onClick = { isFullscreen = false },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Exit Fullscreen")
                    }
                }
            }
        }

        // Immersive Settings Panel
        if (showSettings && !isFullscreen) {
            ImmersiveModeSettingsPanel(
                preferences = uiState.preferences,
                onUpdatePreferences = { viewModel.updatePreferences(it) },
                onDismiss = { showSettings = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveModeSettingsPanel(
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
                text = "Immersive Mode Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Theme selection
            Text("Reading Theme")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.manga_apk.data.ReadingTheme.entries.forEach { theme ->
                    FilterChip(
                        onClick = { onUpdatePreferences(preferences.copy(theme = theme)) },
                        label = { Text(theme.displayName) },
                        selected = preferences.theme == theme
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Font size
            Text("Font Size: ${preferences.fontSize}sp")
            Slider(
                value = preferences.fontSize.toFloat(),
                onValueChange = { newValue ->
                    onUpdatePreferences(preferences.copy(fontSize = newValue.toInt()))
                },
                valueRange = 14f..28f,
                steps = 6
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Line height
            Text("Line Height: ${preferences.lineHeight}sp")
            Slider(
                value = preferences.lineHeight.toFloat(),
                onValueChange = { newValue ->
                    onUpdatePreferences(preferences.copy(lineHeight = newValue.toInt()))
                },
                valueRange = 20f..40f,
                steps = 9
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

private fun generateImmersiveContent(): String {
    return """
        静かな夜の図書館で、古い本のページをめくる音だけが響いていた。
        
        主人公は長い間探していた古文書をついに見つけた。その本には不思議な力が宿っているという伝説があった。
        
        月明かりが窓から差し込み、文字が浮かび上がるように見えた。彼は慎重にページを開き、古代の文字を読み始めた。
        
        「この世界には、まだ知られていない秘密がたくさんある」と彼は心の中でつぶやいた。
        
        本の内容は予想以上に興味深く、時間を忘れて読み続けた。外では風が木々を揺らし、葉っぱのざわめきが聞こえてきた。
        
        これは新しい冒険の始まりだった。
    """.trimIndent()
}