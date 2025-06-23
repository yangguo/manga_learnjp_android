package com.example.manga_apk.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Save
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
    onNavigateBack: () -> Unit,

    onClearPreferences: (() -> Unit)? = null,
    settingsSaved: Boolean = false
) {
    var currentConfig by remember { mutableStateOf(aiConfig) }
    var showOpenAIKey by remember { mutableStateOf(false) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showCustomKey by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Update currentConfig when aiConfig changes
    LaunchedEffect(aiConfig) {
        currentConfig = aiConfig
        println("AISettingsScreen: Received aiConfig - OpenAI key length: ${aiConfig.openaiConfig.apiKey.length}, Gemini key length: ${aiConfig.geminiConfig.apiKey.length}")
        android.util.Log.d("MangaLearnJP", "AISettingsScreen: Received aiConfig - OpenAI key length: ${aiConfig.openaiConfig.apiKey.length}, Gemini key length: ${aiConfig.geminiConfig.apiKey.length}")
        
        // Debug check for configured providers
        val configuredProviders = aiConfig.getConfiguredProviders()
        println("AISettingsScreen: Configured providers: $configuredProviders")
        android.util.Log.d("MangaLearnJP", "AISettingsScreen: Configured providers: $configuredProviders")
    }
    
    // Show snackbar when settings are saved
    LaunchedEffect(settingsSaved) {
        if (settingsSaved) {
            val configuredProviders = currentConfig.getConfiguredProviders()
            val message = if (configuredProviders.isEmpty()) {
                "⚠️ Settings saved but no providers are properly configured!"
            } else {
                "✅ Settings saved successfully with ${configuredProviders.size} configured provider(s)!"
            }
            
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
        }
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
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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
                                            // Don't save automatically - only when save button is clicked
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
                                // Don't save automatically - only when save button is clicked
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
                    // Don't save automatically on every keystroke - only when save button is clicked
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
                            // Don't save automatically - only when save button is clicked
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
                            // Don't save automatically - only when save button is clicked
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
                    // Don't save automatically on every keystroke - only when save button is clicked
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
                            // Don't save automatically - only when save button is clicked
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
                    // Don't save automatically on every keystroke - only when save button is clicked
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
                            // Don't save automatically - only when save button is clicked
                        },
                        label = { Text("API Endpoint") },
                        placeholder = { Text("https://your-api.com/chat or /v1/chat/completions") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = currentConfig.customConfig.model,
                        onValueChange = { newModel ->
                            currentConfig = currentConfig.copy(
                                customConfig = currentConfig.customConfig.copy(model = newModel)
                            )
                            // Don't save automatically - only when save button is clicked
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
                                // Don't save automatically - only when save button is clicked
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
                                // Don't save automatically - only when save button is clicked
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
                                // Don't save automatically - only when save button is clicked
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
            
            // Settings Actions
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Settings Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Save Settings Button

                        Button(
                            onClick = { 
                                // Trim all values before saving
                                val trimmedConfig = currentConfig.copy(
                                    openaiConfig = currentConfig.openaiConfig.copy(
                                        apiKey = currentConfig.openaiConfig.apiKey.trim()
                                    ),
                                    geminiConfig = currentConfig.geminiConfig.copy(
                                        apiKey = currentConfig.geminiConfig.apiKey.trim()
                                    ),
                                    customConfig = currentConfig.customConfig.copy(
                                        apiKey = currentConfig.customConfig.apiKey.trim(),
                                        endpoint = currentConfig.customConfig.endpoint.trim()
                                    )
                                )
                                
                                // Log what we're saving
                                println("AISettingsScreen: Saving config - OpenAI key length: ${trimmedConfig.openaiConfig.apiKey.length}, Gemini key length: ${trimmedConfig.geminiConfig.apiKey.length}")
                                
                                // Update the currentConfig with trimmed values
                                currentConfig = trimmedConfig
                                
                                // Save the trimmed config
                                onConfigUpdate(trimmedConfig)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 6.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "💾 Save All Settings",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = "Click to manually save all your API keys and settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    
                    // Clear Preferences Button
                    if (onClearPreferences != null) {
                        Button(
                            onClick = onClearPreferences,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 6.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "🗑️ Clear All Preferences",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = "⚠️ This will permanently delete all saved settings and API keys",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
    val hasApiKey = apiKey.trim().isNotEmpty()
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (hasApiKey) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (hasApiKey) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (hasApiKey) 4.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (hasApiKey) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (hasApiKey) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "✓ Configured",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
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
                            contentDescription = if (showApiKey) "Hide API key" else "Show API key",
                            tint = if (hasApiKey) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (hasApiKey) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    focusedLabelColor = if (hasApiKey) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
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