package com.example.manga_apk.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.manga_apk.utils.Logger
import androidx.lifecycle.viewModelScope
import com.example.manga_apk.data.*
import com.example.manga_apk.service.AIService
import com.example.manga_apk.service.PanelSegmentationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted

enum class AnalysisMode {
    UPLOAD,
    PANEL_ANALYSIS,
    SIMPLE_ANALYSIS,
    READING_MODE,
    STUDY_MODE,
    SPEED_READING,
    IMMERSIVE_MODE,
    VOCABULARY_FOCUS
}

data class MangaAnalysisUiState(
    val currentMode: AnalysisMode = AnalysisMode.UPLOAD,
    val selectedImage: Bitmap? = null,
    val panels: List<PanelSegment> = emptyList(),
    val overallAnalysis: TextAnalysis? = null,
    val isProcessing: Boolean = false,
    val showSettings: Boolean = false,
    val aiConfig: AIConfig = AIConfig(),
    val error: String? = null,
    val settingsSaved: Boolean = false
)

class MangaAnalysisViewModel(private val context: Context) : ViewModel() {
    
    private val preferencesRepository = PreferencesRepository(context)
    private val _uiState = MutableStateFlow(MangaAnalysisUiState())
    
    val uiState: StateFlow<MangaAnalysisUiState> = combine(
        _uiState,
        preferencesRepository.aiConfigFlow
    ) { state, aiConfig ->
        state.copy(aiConfig = aiConfig)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MangaAnalysisUiState()
    )
    
    private val aiService = AIService()
    private val panelSegmentationService = PanelSegmentationService()
    
    init {
        println("ViewModel: MangaAnalysisViewModel initialized")
        viewModelScope.launch {
            val testResult = aiService.testNetworkConnection()
            println("ViewModel: Network test result: $testResult")
            if (!testResult) {
                println("ViewModel: Network test failed")
            }
        }
    }
    
    fun setMode(mode: AnalysisMode) {
        _uiState.value = _uiState.value.copy(currentMode = mode)
        
        // Auto-process when switching to panel analysis mode
        if (mode == AnalysisMode.PANEL_ANALYSIS && _uiState.value.selectedImage != null && _uiState.value.panels.isEmpty()) {
            segmentPanels()
        }
    }
    
    fun loadImageFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                println("ViewModel: Loading image from URI: $uri")
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                
                if (bitmap != null) {
                    println("ViewModel: Image loaded successfully - Size: ${bitmap.width}x${bitmap.height}, Config: ${bitmap.config}")
                } else {
                    println("ViewModel: Failed to decode bitmap from stream")
                }
                
                _uiState.value = _uiState.value.copy(
                    selectedImage = bitmap,
                    panels = emptyList(),
                    overallAnalysis = null,
                    error = null
                )
                
                // Auto-segment panels if in panel analysis mode
                if (_uiState.value.currentMode == AnalysisMode.PANEL_ANALYSIS) {
                    segmentPanels()
                }
                
            } catch (e: Exception) {
                println("ViewModel: Exception loading image: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load image: ${e.message}"
                )
            }
        }
    }
    
    private fun segmentPanels() {
        val bitmap = _uiState.value.selectedImage ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            
            try {
                val result = panelSegmentationService.segmentPanels(bitmap)
                _uiState.value = _uiState.value.copy(
                    panels = result.panels,
                    isProcessing = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = "Panel segmentation failed: ${e.message}"
                )
            }
        }
    }
    
    fun analyzePanel(panel: PanelSegment) {
        val bitmap = _uiState.value.selectedImage ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            
            try {
                // Extract panel region from the full image
                val panelBitmap = Bitmap.createBitmap(
                    bitmap,
                    panel.x,
                    panel.y,
                    panel.width,
                    panel.height
                )
                
                val result = aiService.analyzeImage(panelBitmap, _uiState.value.aiConfig)
                
                result.fold(
                    onSuccess = { analysis ->
                        val updatedPanels = _uiState.value.panels.map { p ->
                            if (p.id == panel.id) {
                                p.copy(
                                    extractedText = analysis.originalText,
                                    analysis = analysis
                                )
                            } else p
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            panels = updatedPanels,
                            isProcessing = false,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            error = "Analysis failed: ${error.message}"
                        )
                    }
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = "Panel analysis failed: ${e.message}"
                )
            }
        }
    }
    
    private fun validateAnalysisPrerequisites(): String? {
        Logger.logFunctionEntry("MangaAnalysisViewModel", "validateAnalysisPrerequisites")
        println("ViewModel: validateAnalysisPrerequisites() called")
        android.util.Log.d("MangaLearnJP", "ViewModel: validateAnalysisPrerequisites() called")
        
        // Check if image is selected
        val bitmap = _uiState.value.selectedImage
        println("ViewModel: Image check - bitmap is ${if (bitmap != null) "not null (${bitmap.width}x${bitmap.height})" else "null"}")
        android.util.Log.d("MangaLearnJP", "ViewModel: Image check - bitmap is ${if (bitmap != null) "not null (${bitmap.width}x${bitmap.height})" else "null"}")
        
        if (bitmap == null) {
            val errorMsg = "❌ No image selected. Please upload an image first."
            println("ViewModel: Validation failed - $errorMsg")
            android.util.Log.e("MangaLearnJP", "ViewModel: Validation failed - $errorMsg")
            return errorMsg
        }
        
        // Check AI configuration with enhanced debugging
        val currentConfig = _uiState.value.aiConfig
        
        // Enhanced debug logging with trimming check
        val openaiKeyRaw = currentConfig.openaiConfig.apiKey
        val openaiKeyTrimmed = openaiKeyRaw.trim()
        val geminiKeyRaw = currentConfig.geminiConfig.apiKey
        val geminiKeyTrimmed = geminiKeyRaw.trim()
        val customKeyRaw = currentConfig.customConfig.apiKey
        val customKeyTrimmed = customKeyRaw.trim()
        val customEndpointRaw = currentConfig.customConfig.endpoint
        val customEndpointTrimmed = customEndpointRaw.trim()
        
        println("ViewModel: AI Config validation - Primary provider: ${currentConfig.primaryProvider}")
        println("ViewModel: OpenAI - Raw length: ${openaiKeyRaw.length}, Trimmed length: ${openaiKeyTrimmed.length}")
        println("ViewModel: Gemini - Raw length: ${geminiKeyRaw.length}, Trimmed length: ${geminiKeyTrimmed.length}")
        println("ViewModel: Custom - Key length: ${customKeyTrimmed.length}, Endpoint: '${customEndpointTrimmed}'")
        
        // Check for whitespace issues
        if (openaiKeyRaw != openaiKeyTrimmed && openaiKeyRaw.isNotEmpty()) {
            println("ViewModel: ⚠️ WARNING: OpenAI key has leading/trailing whitespace!")
            android.util.Log.w("MangaLearnJP", "OpenAI API key has whitespace - this may cause validation issues")
        }
        if (geminiKeyRaw != geminiKeyTrimmed && geminiKeyRaw.isNotEmpty()) {
            println("ViewModel: ⚠️ WARNING: Gemini key has leading/trailing whitespace!")
            android.util.Log.w("MangaLearnJP", "Gemini API key has whitespace - this may cause validation issues")
        }
        
        // Check individual provider configurations
        val openaiConfigured = openaiKeyTrimmed.isNotEmpty()
        val geminiConfigured = geminiKeyTrimmed.isNotEmpty()
        val customConfigured = customKeyTrimmed.isNotEmpty() && customEndpointTrimmed.isNotEmpty()
        
        println("ViewModel: Provider status - OpenAI: $openaiConfigured, Gemini: $geminiConfigured, Custom: $customConfigured")
        android.util.Log.d("MangaLearnJP", "Provider status - OpenAI: $openaiConfigured, Gemini: $geminiConfigured, Custom: $customConfigured")
        
        // Get configured providers using the built-in method
        val configuredProviders = currentConfig.getConfiguredProviders()
        println("ViewModel: Configured providers from getConfiguredProviders(): $configuredProviders")
        
        // Double-check with manual validation
        val manuallyConfiguredProviders = mutableListOf<AIProvider>()
        if (openaiConfigured) manuallyConfiguredProviders.add(AIProvider.OPENAI)
        if (geminiConfigured) manuallyConfiguredProviders.add(AIProvider.GEMINI)
        if (customConfigured) manuallyConfiguredProviders.add(AIProvider.CUSTOM)
        
        println("ViewModel: Manually detected providers: $manuallyConfiguredProviders")
        
        // If there's a mismatch, log it
        if (configuredProviders.toSet() != manuallyConfiguredProviders.toSet()) {
            println("ViewModel: ⚠️ MISMATCH between built-in and manual provider detection!")
            android.util.Log.w("MangaLearnJP", "Provider detection mismatch - built-in: $configuredProviders, manual: $manuallyConfiguredProviders")
        }
        
        // Use manual validation as fallback if built-in method fails
        val effectiveConfiguredProviders = if (configuredProviders.isNotEmpty()) configuredProviders else manuallyConfiguredProviders
        
        if (effectiveConfiguredProviders.isEmpty()) {
            val troubleshootingInfo = buildString {
                appendLine("\n🔍 Troubleshooting Information:")
                appendLine("• Primary Provider: ${currentConfig.primaryProvider}")
                appendLine("• OpenAI Key: ${if (openaiKeyTrimmed.isEmpty()) "❌ Empty" else "✅ ${openaiKeyTrimmed.length} characters"}")
                appendLine("• Gemini Key: ${if (geminiKeyTrimmed.isEmpty()) "❌ Empty" else "✅ ${geminiKeyTrimmed.length} characters"}")
                appendLine("• Custom Key: ${if (customKeyTrimmed.isEmpty()) "❌ Empty" else "✅ ${customKeyTrimmed.length} characters"}")
                appendLine("• Custom Endpoint: ${if (customEndpointTrimmed.isEmpty()) "❌ Empty" else "✅ Configured"}")
                
                if (openaiKeyRaw != openaiKeyTrimmed || geminiKeyRaw != geminiKeyTrimmed) {
                    appendLine("\n⚠️ Detected whitespace in API keys - this may cause issues!")
                }
                
                appendLine("\n💡 Solutions:")
                appendLine("1. Go to Settings (⚙️ icon)")
                appendLine("2. Enter a valid API key for at least one provider")
                appendLine("3. Make sure there are no extra spaces before/after the key")
                appendLine("4. Try the 'Refresh Config' button in Settings")
                appendLine("5. If issues persist, try 'Clear All Preferences' and reconfigure")
            }
            
            val errorMsg = "❌ No AI providers configured. Please set up at least one API key in Settings." + troubleshootingInfo
            println("ViewModel: Validation failed - No providers configured")
            android.util.Log.e("MangaLearnJP", "Validation failed - No providers configured. OpenAI: ${openaiKeyTrimmed.length} chars, Gemini: ${geminiKeyTrimmed.length} chars")
            return errorMsg
        }
        
        // Validate the primary provider specifically
        val primaryProvider = currentConfig.primaryProvider
        when (primaryProvider) {
            AIProvider.OPENAI -> {
                if (openaiKeyTrimmed.isEmpty()) {
                    return "❌ OpenAI is set as primary provider but API key is missing.\n\n💡 Solution: Add your OpenAI API key in Settings or switch to a different primary provider."
                }
                if (openaiKeyTrimmed.length < 20) {
                    return "❌ OpenAI API key appears to be invalid (too short: ${openaiKeyTrimmed.length} characters).\n\n💡 Solution: Check your OpenAI API key - it should start with 'sk-' and be much longer."
                }
                if (!openaiKeyTrimmed.startsWith("sk-")) {
                    return "❌ OpenAI API key format appears incorrect (should start with 'sk-').\n\n💡 Solution: Verify you copied the correct API key from OpenAI platform."
                }
            }
            AIProvider.GEMINI -> {
                if (geminiKeyTrimmed.isEmpty()) {
                    return "❌ Gemini is set as primary provider but API key is missing.\n\n💡 Solution: Add your Google Gemini API key in Settings or switch to a different primary provider."
                }
                if (geminiKeyTrimmed.length < 20) {
                    return "❌ Gemini API key appears to be invalid (too short: ${geminiKeyTrimmed.length} characters).\n\n💡 Solution: Check your Gemini API key from Google AI Studio."
                }
            }
            AIProvider.CUSTOM -> {
                if (customKeyTrimmed.isEmpty()) {
                    return "❌ Custom API is set as primary provider but API key is missing.\n\n💡 Solution: Add your custom API key in Settings or switch to a different primary provider."
                }
                if (customEndpointTrimmed.isEmpty()) {
                    return "❌ Custom API is set as primary provider but endpoint URL is missing.\n\n💡 Solution: Add your custom API endpoint URL in Settings."
                }
                if (!customEndpointTrimmed.startsWith("http")) {
                    return "❌ Custom API endpoint should be a valid URL (starting with http:// or https://).\n\n💡 Solution: Check your endpoint URL format."
                }
            }
        }
        
        println("ViewModel: ✅ All validations passed - Primary provider: $primaryProvider")
        android.util.Log.d("MangaLearnJP", "✅ All validations passed - Primary provider: $primaryProvider")
        Logger.logViewModelAction("Prerequisites validation passed")
        return null // All validations passed
    }
    
    fun analyzeFullImage() {
        Logger.logFunctionEntry("MangaAnalysisViewModel", "analyzeFullImage")
        println("ViewModel: analyzeFullImage() called")
        android.util.Log.d("MangaLearnJP", "analyzeFullImage() called")
        
        viewModelScope.launch {
            // Use enhanced validation with force reload
            val validationResult = forceReloadAndValidate()
            if (validationResult != null) {
                Logger.logError("analyzeFullImage", "Validation failed: $validationResult")
                println("ViewModel: Validation failed - $validationResult")
                android.util.Log.e("MangaLearnJP", "Validation failed - $validationResult")
                _uiState.value = _uiState.value.copy(error = validationResult)
                return@launch
            }
            
            val bitmap = _uiState.value.selectedImage!!
            val currentConfig = _uiState.value.aiConfig
            
            Logger.logViewModelAction("Starting full image analysis")
            Logger.logConfigChange("Primary Provider", currentConfig.primaryProvider.toString())
            Logger.logConfigChange("Configured Providers", currentConfig.getConfiguredProviders().toString())
            Logger.logImageProcessing("Image ready for analysis", bitmap.width, bitmap.height)
            
            println("ViewModel: Starting analysis with config - Primary: ${currentConfig.primaryProvider}, Providers: ${currentConfig.getConfiguredProviders()}")
            android.util.Log.d("MangaLearnJP", "Starting analysis with primary provider: ${currentConfig.primaryProvider}")
            Logger.logStateChange("MangaAnalysisViewModel", "idle", "processing")
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                error = null
            )
            
            val startTime = System.currentTimeMillis()
            
            try {
                Logger.logAIServiceCall(currentConfig.primaryProvider.toString(), "analyzeImage")
                val result = aiService.analyzeImage(bitmap, currentConfig)
                
                result.fold(
                    onSuccess = { analysis ->
                        val duration = System.currentTimeMillis() - startTime
                        Logger.logPerformance("Image analysis", duration)
                        Logger.i(Logger.Category.AI_SERVICE, "Analysis successful: ${analysis.originalText.take(50)}...")
                        
                        _uiState.value = _uiState.value.copy(
                            overallAnalysis = analysis,
                            isProcessing = false,
                            error = null
                        )
                        Logger.logStateChange("MangaAnalysisViewModel", "processing", "success")
                    },
                    onFailure = { error ->
                        val duration = System.currentTimeMillis() - startTime
                        Logger.logPerformance("Failed image analysis", duration)
                        Logger.logError("analyzeFullImage", error)
                        
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            error = "Analysis failed: ${error.message ?: "Unknown error"}"
                        )
                        Logger.logStateChange("MangaAnalysisViewModel", "processing", "error")
                    }
                )
                
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                Logger.logPerformance("Exception in image analysis", duration)
                Logger.logError("analyzeFullImage", e)
                
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = "Image analysis failed: ${e.message ?: "Unknown error"}"
                )
                Logger.logStateChange("MangaAnalysisViewModel", "processing", "exception")
            }
        }
        
        Logger.logFunctionExit("MangaAnalysisViewModel", "analyzeFullImage")
    }
    
    fun analyzeWord(word: String) {
        // This would be used in reading mode for individual word analysis
        viewModelScope.launch {
            // Implementation for word-level analysis
            // Could use a simpler API call for just vocabulary lookup
        }
    }
    
    fun updateAIConfig(config: AIConfig) {
        println("ViewModel: updateAIConfig called - OpenAI key length: ${config.openaiConfig.apiKey.length}, Gemini key length: ${config.geminiConfig.apiKey.length}")
        android.util.Log.d("MangaLearnJP", "ViewModel: updateAIConfig called - OpenAI key length: ${config.openaiConfig.apiKey.length}, Gemini key length: ${config.geminiConfig.apiKey.length}")
        
        // Validate the config before saving
        val trimmedOpenAI = config.openaiConfig.apiKey.trim()
        val trimmedGemini = config.geminiConfig.apiKey.trim()
        val trimmedCustomKey = config.customConfig.apiKey.trim()
        val trimmedCustomEndpoint = config.customConfig.endpoint.trim()
        
        println("ViewModel: Saving config with trimmed values - OpenAI: ${trimmedOpenAI.length} chars, Gemini: ${trimmedGemini.length} chars")
        
        viewModelScope.launch {
            try {
                // Clear previous saved status
                _uiState.value = _uiState.value.copy(settingsSaved = false)
                
                preferencesRepository.saveAIConfig(config)
                println("ViewModel: AI config saved successfully")
                android.util.Log.d("MangaLearnJP", "ViewModel: AI config saved successfully")
                
                // Set saved status to true
                _uiState.value = _uiState.value.copy(settingsSaved = true)
                
                // Force refresh the UI state to pick up the new config with longer delay
                delay(200) // Increased delay to ensure DataStore has processed the save
                refreshAIConfig()
                
                // Additional verification
                delay(100)
                val verificationConfig = _uiState.value.aiConfig
                println("ViewModel: Post-save verification - OpenAI: ${verificationConfig.openaiConfig.apiKey.trim().length} chars, Gemini: ${verificationConfig.geminiConfig.apiKey.trim().length} chars")
                android.util.Log.d("MangaLearnJP", "Post-save verification - OpenAI: ${verificationConfig.openaiConfig.apiKey.trim().length} chars")
                
                // Clear saved status after a delay
                delay(2000)
                _uiState.value = _uiState.value.copy(settingsSaved = false)
                
            } catch (e: Exception) {
                println("ViewModel: Error saving AI config: ${e.message}")
                android.util.Log.e("MangaLearnJP", "Error saving AI config", e)
                _uiState.value = _uiState.value.copy(settingsSaved = false)
            }
        }
    }
    
    fun refreshAIConfig() {
        println("ViewModel: Manually refreshing AI config")
        android.util.Log.d("MangaLearnJP", "ViewModel: Manually refreshing AI config")
        
        viewModelScope.launch {
            try {
                // Force collect the latest value from DataStore
                preferencesRepository.aiConfigFlow.collect { latestConfig ->
                    println("ViewModel: Collected latest config - OpenAI: ${latestConfig.openaiConfig.apiKey.trim().length} chars, Gemini: ${latestConfig.geminiConfig.apiKey.trim().length} chars")
                    android.util.Log.d("MangaLearnJP", "Latest config collected - OpenAI: ${latestConfig.openaiConfig.apiKey.trim().length} chars, Gemini: ${latestConfig.geminiConfig.apiKey.trim().length} chars")
                    
                    // Log configured providers
                    val configuredProviders = latestConfig.getConfiguredProviders()
                    println("ViewModel: Configured providers after refresh: $configuredProviders")
                    android.util.Log.d("MangaLearnJP", "Configured providers after refresh: $configuredProviders")
                    
                    // Only collect once for refresh
                    return@collect
                }
            } catch (e: Exception) {
                println("ViewModel: Error refreshing config: ${e.message}")
                android.util.Log.e("MangaLearnJP", "Error refreshing AI config", e)
            }
        }
    }
    
    // Helper function to force reload configuration and validate
    suspend fun forceReloadAndValidate(): String? {
        println("ViewModel: Force reloading configuration for validation")
        android.util.Log.d("MangaLearnJP", "Force reloading configuration for validation")
        
        return try {
            // Wait a bit for any pending saves to complete
            delay(300)
            
            // Force refresh
            refreshAIConfig()
            delay(200)
            
            // Now validate with the refreshed config
            validateAnalysisPrerequisites()
        } catch (e: Exception) {
            println("ViewModel: Error during force reload and validate: ${e.message}")
            android.util.Log.e("MangaLearnJP", "Error during force reload and validate", e)
            "❌ Error validating configuration: ${e.message}"
        }
    }
    
    fun clearAllPreferences() {
        println("ViewModel: Clearing all preferences")
        android.util.Log.d("MangaLearnJP", "ViewModel: Clearing all preferences")
        viewModelScope.launch {
            preferencesRepository.clearAllPreferences()
            _uiState.value = _uiState.value.copy(
                error = "All preferences cleared. Please reconfigure your API keys."
            )
        }
    }
    
    fun toggleSettings() {
        _uiState.value = _uiState.value.copy(
            showSettings = !_uiState.value.showSettings
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun quickAnalysis() {
        Logger.logFunctionEntry("MangaAnalysisViewModel", "quickAnalysis")
        
        if (_uiState.value.selectedImage == null) {
            Logger.logError("quickAnalysis", "No image selected")
            _uiState.value = _uiState.value.copy(
                error = "Please select an image first"
            )
            return
        }
        
        Logger.logStateChange("MangaAnalysisViewModel", "currentMode", "SIMPLE_ANALYSIS")
        _uiState.value = _uiState.value.copy(
            currentMode = AnalysisMode.SIMPLE_ANALYSIS,
            error = null
        )
        
        Logger.d(Logger.Category.VIEWMODEL, "Starting analysis with fallback")
        analyzeWithFallback()
    }

    fun analyzeWithFallback() {
        Logger.logFunctionEntry("MangaAnalysisViewModel", "analyzeWithFallback")
        viewModelScope.launch {
            try {
                Logger.d(Logger.Category.VIEWMODEL, "Calling analyzeFullImage from analyzeWithFallback")
                analyzeFullImage()
            } catch (e: Exception) {
                Logger.logError("analyzeWithFallback", e)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = "Analysis failed: ${e.message}"
                )
            }
        }
    }
    
    fun testAnalysis() {
        println("ViewModel: Running test analysis")
        val config = _uiState.value.aiConfig
        
        // Enhanced debugging with raw vs trimmed values
        val openaiKeyRaw = config.openaiConfig.apiKey
        val openaiKeyTrimmed = openaiKeyRaw.trim()
        val geminiKeyRaw = config.geminiConfig.apiKey
        val geminiKeyTrimmed = geminiKeyRaw.trim()
        val customKeyRaw = config.customConfig.apiKey
        val customKeyTrimmed = customKeyRaw.trim()
        val customEndpointRaw = config.customConfig.endpoint
        val customEndpointTrimmed = customEndpointRaw.trim()
        
        val debugInfo = buildString {
            appendLine("=== DEBUG INFO ===")
            appendLine("Primary Provider: ${config.primaryProvider}")
            appendLine("Fallback Enabled: ${config.enableFallback}")
            
            appendLine("\n=== RAW API KEY ANALYSIS ===")
            appendLine("OpenAI Key - Raw length: ${openaiKeyRaw.length}, Trimmed length: ${openaiKeyTrimmed.length}")
            if (openaiKeyRaw != openaiKeyTrimmed) {
                appendLine("  ⚠️ OpenAI key has whitespace! Raw: '${openaiKeyRaw.take(20)}...', Trimmed: '${openaiKeyTrimmed.take(20)}...'")
            }
            appendLine("Gemini Key - Raw length: ${geminiKeyRaw.length}, Trimmed length: ${geminiKeyTrimmed.length}")
            if (geminiKeyRaw != geminiKeyTrimmed) {
                appendLine("  ⚠️ Gemini key has whitespace! Raw: '${geminiKeyRaw.take(20)}...', Trimmed: '${geminiKeyTrimmed.take(20)}...'")
            }
            appendLine("Custom Key - Raw length: ${customKeyRaw.length}, Trimmed length: ${customKeyTrimmed.length}")
            appendLine("Custom Endpoint - Raw: '${customEndpointRaw}', Trimmed: '${customEndpointTrimmed}'")
            
            // Show configured providers
            val configuredProviders = config.getConfiguredProviders()
            appendLine("\n=== CONFIGURED PROVIDERS ===")
            appendLine("Detected Providers: $configuredProviders")
            
            // Show details for each provider
            appendLine("\n=== PROVIDER DETAILS ===")
            appendLine("OpenAI Configured: ${config.isProviderConfigured(AIProvider.OPENAI)}")
            if (openaiKeyTrimmed.isNotEmpty()) {
                appendLine("  OpenAI API Key: ${openaiKeyTrimmed.take(10)}... (${openaiKeyTrimmed.length} chars)")
                appendLine("  OpenAI Vision Model: ${config.openaiConfig.visionModel}")
            }
            
            appendLine("Gemini Configured: ${config.isProviderConfigured(AIProvider.GEMINI)}")
            if (geminiKeyTrimmed.isNotEmpty()) {
                appendLine("  Gemini API Key: ${geminiKeyTrimmed.take(10)}... (${geminiKeyTrimmed.length} chars)")
                appendLine("  Gemini Model: ${config.geminiConfig.model}")
            }
            
            appendLine("Custom Configured: ${config.isProviderConfigured(AIProvider.CUSTOM)}")
            if (customKeyTrimmed.isNotEmpty()) {
                appendLine("  Custom API Key: ${customKeyTrimmed.take(10)}... (${customKeyTrimmed.length} chars)")
                appendLine("  Custom Endpoint: ${customEndpointTrimmed}")
                appendLine("  Custom Model: ${config.customConfig.model}")
            }
            
            // Show validation results
            appendLine("\n=== VALIDATION RESULTS ===")
            val validationResult = validateAnalysisPrerequisites()
            if (validationResult != null) {
                appendLine("❌ Validation Failed: $validationResult")
            } else {
                appendLine("✅ Validation Passed: All prerequisites met")
            }
            
            // Show fix suggestions
            if (configuredProviders.isEmpty()) {
                appendLine("\n=== FIX SUGGESTIONS ===")
                if (openaiKeyTrimmed.isEmpty() && geminiKeyTrimmed.isEmpty() && customKeyTrimmed.isEmpty()) {
                    appendLine("• No API keys detected. Please add at least one API key in Settings.")
                } else {
                    appendLine("• API keys detected but not recognized as configured. This might be a validation bug.")
                    appendLine("• Try re-entering your API key in Settings.")
                    appendLine("• Make sure there are no extra spaces before or after the API key.")
                }
            }
        }
        
        println(debugInfo)
        android.util.Log.d("MangaLearnJP", debugInfo)
        
        _uiState.value = _uiState.value.copy(
            error = debugInfo
        )
    }
}