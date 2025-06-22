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
    val error: String? = null
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
        
        // Check AI configuration
        val currentConfig = _uiState.value.aiConfig
        val configuredProviders = currentConfig.getConfiguredProviders()
        
        // Enhanced debug logging with trimming check
        val openaiKeyTrimmed = currentConfig.openaiConfig.apiKey.trim()
        val geminiKeyTrimmed = currentConfig.geminiConfig.apiKey.trim()
        val customKeyTrimmed = currentConfig.customConfig.apiKey.trim()
        val customEndpointTrimmed = currentConfig.customConfig.endpoint.trim()
        
        println("ViewModel: AI Config check - Primary provider: ${currentConfig.primaryProvider}, Configured providers: $configuredProviders")
        println("ViewModel: Raw OpenAI key length: ${currentConfig.openaiConfig.apiKey.length}, trimmed: ${openaiKeyTrimmed.length}")
        println("ViewModel: Raw Gemini key length: ${currentConfig.geminiConfig.apiKey.length}, trimmed: ${geminiKeyTrimmed.length}")
        println("ViewModel: Raw Custom key length: ${currentConfig.customConfig.apiKey.length}, trimmed: ${customKeyTrimmed.length}")
        println("ViewModel: Custom endpoint: '${currentConfig.customConfig.endpoint}', trimmed: '${customEndpointTrimmed}'")
        
        android.util.Log.d("MangaLearnJP", "ViewModel: AI Config check - Primary provider: ${currentConfig.primaryProvider}, Configured providers: $configuredProviders")
        android.util.Log.d("MangaLearnJP", "ViewModel: API key status - OpenAI: ${if (openaiKeyTrimmed.isNotEmpty()) "${openaiKeyTrimmed.length} chars" else "empty"}, Gemini: ${if (geminiKeyTrimmed.isNotEmpty()) "${geminiKeyTrimmed.length} chars" else "empty"}")
        
        // Check individual provider configurations with detailed logging
        val openaiConfigured = openaiKeyTrimmed.isNotEmpty()
        val geminiConfigured = geminiKeyTrimmed.isNotEmpty()
        val customConfigured = customKeyTrimmed.isNotEmpty() && customEndpointTrimmed.isNotEmpty()
        
        println("ViewModel: Manual provider check - OpenAI: $openaiConfigured, Gemini: $geminiConfigured, Custom: $customConfigured")
        
        // Also check using the built-in method
        val openaiConfiguredBuiltIn = currentConfig.isProviderConfigured(AIProvider.OPENAI)
        val geminiConfiguredBuiltIn = currentConfig.isProviderConfigured(AIProvider.GEMINI)
        val customConfiguredBuiltIn = currentConfig.isProviderConfigured(AIProvider.CUSTOM)
        
        println("ViewModel: Built-in provider check - OpenAI: $openaiConfiguredBuiltIn, Gemini: $geminiConfiguredBuiltIn, Custom: $customConfiguredBuiltIn")
        
        if (configuredProviders.isEmpty()) {
            val errorMsg = "❌ No AI providers configured. Please set up at least one API key in Settings:\n" +
                    "• OpenAI API key for GPT-4 Vision\n" +
                    "• Google Gemini API key\n" +
                    "• Or configure a custom OpenAI-compatible API\n\n" +
                    "Debug info: OpenAI key length=${openaiKeyTrimmed.length}, Gemini key length=${geminiKeyTrimmed.length}, Custom key length=${customKeyTrimmed.length}"
            println("ViewModel: Validation failed - $errorMsg")
            android.util.Log.e("MangaLearnJP", "ViewModel: Validation failed - $errorMsg")
            return errorMsg
        }
        
        // Validate specific provider configurations
        when (currentConfig.primaryProvider) {
            AIProvider.OPENAI -> {
                if (openaiKeyTrimmed.isEmpty()) {
                    return "❌ OpenAI API key is missing. Please add your OpenAI API key in Settings."
                }
                if (openaiKeyTrimmed.length < 20) {
                    return "❌ OpenAI API key appears to be invalid (too short). Please check your API key in Settings."
                }
            }
            AIProvider.GEMINI -> {
                if (geminiKeyTrimmed.isEmpty()) {
                    return "❌ Gemini API key is missing. Please add your Google Gemini API key in Settings."
                }
            }
            AIProvider.CUSTOM -> {
                if (customKeyTrimmed.isEmpty()) {
                    return "❌ Custom API key is missing. Please configure your custom API in Settings."
                }
                if (customEndpointTrimmed.isEmpty()) {
                    return "❌ Custom API endpoint is missing. Please configure your custom API endpoint in Settings."
                }
            }
        }
        
        println("ViewModel: All validations passed")
        android.util.Log.d("MangaLearnJP", "ViewModel: All validations passed")
        Logger.logViewModelAction("Prerequisites validation passed")
        return null // All validations passed
    }
    
    fun analyzeFullImage() {
        Logger.logFunctionEntry("MangaAnalysisViewModel", "analyzeFullImage")
        
        // Validate prerequisites first
        val validationResult = validateAnalysisPrerequisites()
        if (validationResult != null) {
            Logger.logError("analyzeFullImage", "Validation failed: $validationResult")
            _uiState.value = _uiState.value.copy(error = validationResult)
            return
        }
        
        val bitmap = _uiState.value.selectedImage!!
        val currentConfig = _uiState.value.aiConfig
        
        Logger.logViewModelAction("Starting full image analysis")
        Logger.logConfigChange("Primary Provider", currentConfig.primaryProvider.toString())
        Logger.logConfigChange("Configured Providers", currentConfig.getConfiguredProviders().toString())
        Logger.logImageProcessing("Image ready for analysis", bitmap.width, bitmap.height)
        
        // Validate configuration before proceeding
        val configuredProviders = currentConfig.getConfiguredProviders()
        if (configuredProviders.isEmpty()) {
            Logger.logError("analyzeFullImage", "No providers configured")
            _uiState.value = _uiState.value.copy(
                error = "No AI providers are configured. Please configure at least one provider in settings."
            )
            return
        }
        
        viewModelScope.launch {
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
        viewModelScope.launch {
            preferencesRepository.saveAIConfig(config)
            println("ViewModel: AI config saved successfully")
            android.util.Log.d("MangaLearnJP", "ViewModel: AI config saved successfully")
            
            // Force refresh the UI state to pick up the new config
            delay(100) // Small delay to ensure DataStore has processed the save
            refreshAIConfig()
        }
    }
    
    fun refreshAIConfig() {
        println("ViewModel: Manually refreshing AI config")
        android.util.Log.d("MangaLearnJP", "ViewModel: Manually refreshing AI config")
        // The StateFlow will automatically update when the DataStore emits new values
        // This function can be used to trigger debug logging
        viewModelScope.launch {
            val currentConfig = _uiState.value.aiConfig
            println("ViewModel: Current config after refresh - OpenAI: ${currentConfig.openaiConfig.apiKey.length} chars, Gemini: ${currentConfig.geminiConfig.apiKey.length} chars")
            android.util.Log.d("MangaLearnJP", "ViewModel: Current config after refresh - OpenAI: ${currentConfig.openaiConfig.apiKey.length} chars, Gemini: ${currentConfig.geminiConfig.apiKey.length} chars")
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