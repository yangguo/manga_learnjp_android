package com.example.manga_apk.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.manga_apk.data.AIConfig
import com.example.manga_apk.data.AIProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    aiConfig: AIConfig,
    onConfigUpdate: (AIConfig) -> Unit,
    onNavigateBack: () -> Unit
) {
    var currentConfig by remember { mutableStateOf(aiConfig) }
    var showApiKey by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Provider Selection
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AI Provider",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column(
                        modifier = Modifier.selectableGroup()
                    ) {
                        AIProvider.values().forEach { provider ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = currentConfig.provider == provider,
                                        onClick = {
                                            currentConfig = currentConfig.copy(provider = provider)
                                            onConfigUpdate(currentConfig)
                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentConfig.provider == provider,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = provider.displayName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = getProviderDescription(provider),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // API Key Configuration
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "API Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = currentConfig.apiKey,
                        onValueChange = { newKey ->
                            currentConfig = currentConfig.copy(apiKey = newKey)
                            onConfigUpdate(currentConfig)
                        },
                        label = { Text("API Key") },
                        placeholder = { Text("Enter your ${currentConfig.provider.displayName} API key") },
                        visualTransformation = if (showApiKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showApiKey) "Hide API key" else "Show API key"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (currentConfig.provider == AIProvider.CUSTOM) {
                        OutlinedTextField(
                            value = currentConfig.baseUrl,
                            onValueChange = { newUrl ->
                                currentConfig = currentConfig.copy(baseUrl = newUrl)
                                onConfigUpdate(currentConfig)
                            },
                            label = { Text("Base URL") },
                            placeholder = { Text("https://api.example.com/v1") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = currentConfig.model,
                            onValueChange = { newModel ->
                                currentConfig = currentConfig.copy(model = newModel)
                                onConfigUpdate(currentConfig)
                            },
                            label = { Text("Model Name") },
                            placeholder = { Text("gpt-4-vision-preview") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // Analysis Settings
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Analysis Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Include Grammar Analysis",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Analyze grammar patterns in text",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = currentConfig.includeGrammar,
                            onCheckedChange = { checked ->
                                currentConfig = currentConfig.copy(includeGrammar = checked)
                                onConfigUpdate(currentConfig)
                            }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Include Vocabulary Breakdown",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Provide detailed word analysis",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = currentConfig.includeVocabulary,
                            onCheckedChange = { checked ->
                                currentConfig = currentConfig.copy(includeVocabulary = checked)
                                onConfigUpdate(currentConfig)
                            }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Include Translation",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Provide English translation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = currentConfig.includeTranslation,
                            onCheckedChange = { checked ->
                                currentConfig = currentConfig.copy(includeTranslation = checked)
                                onConfigUpdate(currentConfig)
                            }
                        )
                    }
                }
            }
            
            // Help Text
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "How to get API keys:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• OpenAI: Visit platform.openai.com and create an API key\n" +
                                "• Google Gemini: Visit ai.google.dev and get your API key\n" +
                                "• Custom: Use any OpenAI-compatible API endpoint",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun getProviderDescription(provider: AIProvider): String {
    return when (provider) {
        AIProvider.OPENAI -> "GPT-4 Vision for high-quality text analysis"
        AIProvider.GEMINI -> "Google's Gemini Pro Vision model"
        AIProvider.CUSTOM -> "Use your own OpenAI-compatible API"
    }
}