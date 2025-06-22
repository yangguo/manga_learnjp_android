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
import com.example.manga_apk.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    aiConfig: AIConfig,
    onConfigUpdate: (AIConfig) -> Unit,
    onNavigateBack: () -> Unit
) {
    var currentConfig by remember { mutableStateOf(aiConfig) }
    var showOpenAIKey by remember { mutableStateOf(false) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showCustomKey by remember { mutableStateOf(false) }
    
    // Update currentConfig when aiConfig changes
    LaunchedEffect(aiConfig) {
        currentConfig = aiConfig
        println("AISettingsScreen: Received aiConfig - OpenAI key length: ${aiConfig.openaiConfig.apiKey.length}, Gemini key length: ${aiConfig.geminiConfig.apiKey.length}")
        android.util.Log.d("MangaLearnJP", "AISettingsScreen: Received aiConfig - OpenAI key length: ${aiConfig.openaiConfig.apiKey.length}, Gemini key length: ${aiConfig.geminiConfig.apiKey.length}")
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            // Primary Provider Selection
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Primary AI Provider",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Select your preferred AI provider. If fallback is enabled, other configured providers will be tried if the primary fails.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Column(
                        modifier = Modifier.selectableGroup()
                    ) {
                        AIProvider.values().forEach { provider ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = currentConfig.primaryProvider == provider,
                                        onClick = {
                                            currentConfig = currentConfig.copy(primaryProvider = provider)
                                            onConfigUpdate(currentConfig)
                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentConfig.primaryProvider == provider,
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
            
            // Fallback Settings
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Fallback",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Try other configured providers if the primary fails",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = currentConfig.enableFallback,
                            onCheckedChange = { enabled ->
                                currentConfig = currentConfig.copy(enableFallback = enabled)
                                onConfigUpdate(currentConfig)
                            }
                        )
                    }
                }
            }
            
            // OpenAI Configuration
            ProviderConfigCard(
                title = "OpenAI Configuration",
                description = "Configure OpenAI GPT-4 Vision API",
                apiKey = currentConfig.openaiConfig.apiKey,
                onApiKeyChange = { newKey ->
                    val trimmedKey = newKey.trim()
                    println("AISettingsScreen: OpenAI API key updated - length: ${trimmedKey.length}")
                    android.util.Log.d("MangaLearnJP", "AISettingsScreen: OpenAI API key updated - length: ${trimmedKey.length}")
                    currentConfig = currentConfig.copy(
                        openaiConfig = currentConfig.openaiConfig.copy(apiKey = trimmedKey)
                    )
                    onConfigUpdate(currentConfig)
                },
                showApiKey = showOpenAIKey,
                onToggleShowApiKey = { showOpenAIKey = !showOpenAIKey },
                additionalFields = {
                    OutlinedTextField(
                        value = currentConfig.openaiConfig.textModel,
                        onValueChange = { newModel ->
                            currentConfig = currentConfig.copy(
                                openaiConfig = currentConfig.openaiConfig.copy(textModel = newModel)
                            )
                            onConfigUpdate(currentConfig)
                        },
                        label = { Text("Text Model") },
                        placeholder = { Text("gpt-4-turbo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = currentConfig.openaiConfig.visionModel,
                        onValueChange = { newModel ->
                            currentConfig = currentConfig.copy(
                                openaiConfig = currentConfig.openaiConfig.copy(visionModel = newModel)
                            )
                            onConfigUpdate(currentConfig)
                        },
                        label = { Text("Vision Model") },
                        placeholder = { Text("gpt-4o") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
            
            // Gemini Configuration
            ProviderConfigCard(
                title = "Google Gemini Configuration",
                description = "Configure Google Gemini Pro Vision API",
                apiKey = currentConfig.geminiConfig.apiKey,
                onApiKeyChange = { newKey ->
                    val trimmedKey = newKey.trim()
                    println("AISettingsScreen: Gemini API key updated - length: ${trimmedKey.length}")
                    android.util.Log.d("MangaLearnJP", "AISettingsScreen: Gemini API key updated - length: ${trimmedKey.length}")
                    currentConfig = currentConfig.copy(
                        geminiConfig = currentConfig.geminiConfig.copy(apiKey = trimmedKey)
                    )
                    onConfigUpdate(currentConfig)
                },
                showApiKey = showGeminiKey,
                onToggleShowApiKey = { showGeminiKey = !showGeminiKey },
                additionalFields = {
                    OutlinedTextField(
                        value = currentConfig.geminiConfig.model,
                        onValueChange = { newModel ->
                            currentConfig = currentConfig.copy(
                                geminiConfig = currentConfig.geminiConfig.copy(model = newModel)
                            )
                            onConfigUpdate(currentConfig)
                        },
                        label = { Text("Model") },
                        placeholder = { Text("gemini-1.5-pro") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
            
            // Custom API Configuration
            ProviderConfigCard(
                title = "Custom API Configuration",
                description = "Configure your own OpenAI-compatible API",
                apiKey = currentConfig.customConfig.apiKey,
                onApiKeyChange = { newKey ->
                    val trimmedKey = newKey.trim()
                    println("AISettingsScreen: Custom API key updated - length: ${trimmedKey.length}")
                    android.util.Log.d("MangaLearnJP", "AISettingsScreen: Custom API key updated - length: ${trimmedKey.length}")
                    currentConfig = currentConfig.copy(
                        customConfig = currentConfig.customConfig.copy(apiKey = trimmedKey)
                    )
                    onConfigUpdate(currentConfig)
                },
                showApiKey = showCustomKey,
                onToggleShowApiKey = { showCustomKey = !showCustomKey },
                additionalFields = {
                    OutlinedTextField(
                        value = currentConfig.customConfig.endpoint,
                        onValueChange = { newEndpoint ->
                            currentConfig = currentConfig.copy(
                                customConfig = currentConfig.customConfig.copy(endpoint = newEndpoint)
                            )
                            onConfigUpdate(currentConfig)
                        },
                        label = { Text("API Endpoint") },
                        placeholder = { Text("https://api.example.com/v1/chat/completions") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = currentConfig.customConfig.model,
                        onValueChange = { newModel ->
                            currentConfig = currentConfig.copy(
                                customConfig = currentConfig.customConfig.copy(model = newModel)
                            )
                            onConfigUpdate(currentConfig)
                        },
                        label = { Text("Model Name") },
                        placeholder = { Text("gpt-4o") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
            
            // Analysis Options
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Analysis Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include Grammar Analysis")
                        Switch(
                            checked = currentConfig.includeGrammar,
                            onCheckedChange = { enabled ->
                                currentConfig = currentConfig.copy(includeGrammar = enabled)
                                onConfigUpdate(currentConfig)
                            }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include Vocabulary Analysis")
                        Switch(
                            checked = currentConfig.includeVocabulary,
                            onCheckedChange = { enabled ->
                                currentConfig = currentConfig.copy(includeVocabulary = enabled)
                                onConfigUpdate(currentConfig)
                            }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include Translation")
                        Switch(
                            checked = currentConfig.includeTranslation,
                            onCheckedChange = { enabled ->
                                currentConfig = currentConfig.copy(includeTranslation = enabled)
                                onConfigUpdate(currentConfig)
                            }
                        )
                    }
                }
            }
            
            // Help Section
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "How to get API keys:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "• OpenAI: Visit platform.openai.com and create an API key\n" +
                                "• Google Gemini: Visit ai.google.dev and get your API key\n" +
                                "• Custom API: Use any OpenAI-compatible API endpoint",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ProviderConfigCard(
    title: String,
    description: String,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    showApiKey: Boolean,
    onToggleShowApiKey: () -> Unit,
    additionalFields: @Composable ColumnScope.() -> Unit = {}
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("API Key") },
                placeholder = { Text("Enter your API key") },
                visualTransformation = if (showApiKey) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = onToggleShowApiKey) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showApiKey) "Hide API key" else "Show API key"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            
            additionalFields()
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