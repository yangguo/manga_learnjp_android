package com.example.manga_apk.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.manga_apk.viewmodel.ReadingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedReadingScreen(
    viewModel: ReadingViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isPlaying by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Speed Reading Mode") },
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

        // Speed Reading Controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Reading Speed: ${uiState.preferences.speedReadingSettings?.wordsPerMinute ?: 300} WPM",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Play/Pause Button
                FloatingActionButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (isPlaying) "Reading..." else "Tap to start speed reading",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Reading Content
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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.content?.title ?: "Speed Reading Content",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Current word/phrase display
                Text(
                    text = "現在の単語", // Current word placeholder
                    style = MaterialTheme.typography.displayMedium,
                    fontSize = (uiState.preferences.fontSize + 20).sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Translation: Current word",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Speed Reading Settings Panel
        if (showSettings) {
            SpeedReadingSettingsPanel(
                preferences = uiState.preferences,
                onUpdatePreferences = { viewModel.updatePreferences(it) },
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
fun SpeedReadingSettingsPanel(
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
                text = "Speed Reading Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Words per minute slider
            Text("Words per Minute: ${preferences.speedReadingSettings?.wordsPerMinute ?: 300}")
            Slider(
                value = (preferences.speedReadingSettings?.wordsPerMinute ?: 300).toFloat(),
                onValueChange = { newValue ->
                    val newSettings = preferences.speedReadingSettings?.copy(
                        wordsPerMinute = newValue.toInt()
                    ) ?: com.example.manga_apk.data.SpeedReadingSettings(wordsPerMinute = newValue.toInt())
                    onUpdatePreferences(preferences.copy(speedReadingSettings = newSettings))
                },
                valueRange = 100f..1000f,
                steps = 17
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Font size slider
            Text("Font Size: ${preferences.fontSize}sp")
            Slider(
                value = preferences.fontSize.toFloat(),
                onValueChange = { newValue ->
                    onUpdatePreferences(preferences.copy(fontSize = newValue.toInt()))
                },
                valueRange = 12f..32f,
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