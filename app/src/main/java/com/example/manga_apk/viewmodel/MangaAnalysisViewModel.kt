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
    READING_MODE
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
            println("ViewModel: Network test result: ${testResult.isSuccess}")
            if (testResult.isFailure) {
                println("ViewModel: Network test error: ${testResult.exceptionOrNull()?.message}")
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
                val panels = panelSegmentationService.segmentPanels(bitmap)
                _uiState.value = _uiState.value.copy(
                    panels = panels,
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
    
    fun analyzeFullImage() {
        Logger.logFunctionEntry("MangaAnalysisViewModel", "analyzeFullImage")
        
        val bitmap = _uiState.value.selectedImage
        if (bitmap == null) {
            Logger.logError("analyzeFullImage", "No image selected for analysis")
            _uiState.value = _uiState.value.copy(
                error = "No image selected. Please upload an image first."
            )
            return
        }
        
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
        viewModelScope.launch {
            preferencesRepository.saveAIConfig(config)
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
        println("Quick analysis triggered - current mode: ${_uiState.value.currentMode}")
        _uiState.value = _uiState.value.copy(currentMode = AnalysisMode.SIMPLE_ANALYSIS)
        println("Mode set to SIMPLE_ANALYSIS")
        analyzeFullImage()
    }
    
    fun testAnalysis() {
        println("ViewModel: Running test analysis")
        val config = _uiState.value.aiConfig
        val debugInfo = buildString {
            appendLine("=== DEBUG INFO ===")
            appendLine("Primary Provider: ${config.primaryProvider}")
            appendLine("Fallback Enabled: ${config.enableFallback}")
            
            // Show configured providers
            val configuredProviders = config.getConfiguredProviders()
            appendLine("Configured Providers: $configuredProviders")
            
            // Show details for each configured provider
            if (config.isProviderConfigured(AIProvider.OPENAI)) {
                appendLine("OpenAI API Key: ${config.openaiConfig.apiKey.take(10)}... (${config.openaiConfig.apiKey.length} chars)")
                appendLine("OpenAI Vision Model: ${config.openaiConfig.visionModel}")
            }
            
            if (config.isProviderConfigured(AIProvider.GEMINI)) {
                appendLine("Gemini API Key: ${config.geminiConfig.apiKey.take(10)}... (${config.geminiConfig.apiKey.length} chars)")
                appendLine("Gemini Model: ${config.geminiConfig.model}")
            }
            
            if (config.isProviderConfigured(AIProvider.CUSTOM)) {
                appendLine("Custom API Key: ${config.customConfig.apiKey.take(10)}... (${config.customConfig.apiKey.length} chars)")
                appendLine("Custom Model: ${config.customConfig.model}")
                appendLine("Custom Endpoint: ${config.customConfig.endpoint}")
            }
            
            appendLine("Image: ${_uiState.value.selectedImage != null}")
            if (_uiState.value.selectedImage != null) {
                appendLine("Image Size: ${_uiState.value.selectedImage!!.width}x${_uiState.value.selectedImage!!.height}")
            }
            appendLine("==================")
        }
        
        // Create a test analysis to verify the UI is working
        val testAnalysis = TextAnalysis(
            originalText = "こんにちは",
            vocabulary = listOf(
                VocabularyItem(
                    word = "こんにちは",
                    reading = "こんにちは", 
                    meaning = "Hello",
                    partOfSpeech = "Greeting",
                    jlptLevel = "N5",
                    difficulty = 1
                )
            ),
            grammarPatterns = listOf(
                GrammarPattern(
                    pattern = "Greeting phrase",
                    explanation = "Basic greeting in Japanese",
                    example = "こんにちは",
                    difficulty = "beginner"
                )
            ),
            translation = "Hello\n\n$debugInfo",
            context = "This is a test analysis to verify the UI is working properly."
        )
        
        _uiState.value = _uiState.value.copy(
            currentMode = AnalysisMode.SIMPLE_ANALYSIS,
            overallAnalysis = testAnalysis,
            isProcessing = false,
            error = null
        )
    }
    
    fun analyzeWithFallback() {
        Logger.logFunctionEntry("MangaAnalysisViewModel", "analyzeWithFallback")
        
        val currentConfig = _uiState.value.aiConfig
        val selectedImage = _uiState.value.selectedImage
        
        Logger.logConfigChange("Primary Provider", currentConfig.primaryProvider.toString())
        Logger.logConfigChange("Configured Providers", currentConfig.getConfiguredProviders().toString())
        Logger.logConfigChange("Fallback Enabled", currentConfig.enableFallback.toString())
        
        // Show which model will be used based on provider
        when (currentConfig.primaryProvider) {
            AIProvider.OPENAI -> {
                Logger.logConfigChange("OpenAI Vision Model", currentConfig.openaiConfig.visionModel)
                Logger.logConfigChange("OpenAI API Key Configured", currentConfig.openaiConfig.apiKey.isNotEmpty().toString())
            }
            AIProvider.GEMINI -> {
                Logger.logConfigChange("Gemini Model", "gemini-1.5-pro (fixed)")
                Logger.logConfigChange("Gemini API Key Configured", currentConfig.geminiConfig.apiKey.isNotEmpty().toString())
            }
            AIProvider.CUSTOM -> {
                val modelToUse = if (currentConfig.customConfig.model.isNotEmpty()) {
                    currentConfig.customConfig.model
                } else {
                    "gpt-4o" // fallback model
                }
                Logger.logConfigChange("Custom Model", modelToUse)
                Logger.logConfigChange("Custom Endpoint", currentConfig.customConfig.endpoint)
                Logger.logConfigChange("Custom API Key Configured", currentConfig.customConfig.apiKey.isNotEmpty().toString())
            }
        }
        
        if (selectedImage != null) {
            Logger.logImageProcessing("Image available for analysis", selectedImage.width, selectedImage.height)
        } else {
            Logger.logError("analyzeWithFallback", "No image selected")
        }
        
        Logger.logStateChange("MangaAnalysisViewModel", "current processing state", _uiState.value.isProcessing.toString())
        
        // Directly call analyzeFullImage which has proper validation
        Logger.logViewModelAction("Delegating to analyzeFullImage")
        analyzeFullImage()
        
        Logger.logFunctionExit("MangaAnalysisViewModel", "analyzeWithFallback")
    }
}