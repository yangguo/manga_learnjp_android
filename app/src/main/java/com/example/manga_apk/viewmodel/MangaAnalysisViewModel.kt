package com.example.manga_apk.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
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
        val bitmap = _uiState.value.selectedImage
        if (bitmap == null) {
            println("No image selected for analysis")
            _uiState.value = _uiState.value.copy(
                error = "No image selected. Please upload an image first."
            )
            return
        }
        
        val currentConfig = _uiState.value.aiConfig
        
        println("Starting full image analysis - Provider: ${currentConfig.provider}, API Key present: ${currentConfig.apiKey.isNotEmpty()}")
        
        // Validate configuration before proceeding
        if (currentConfig.apiKey.isEmpty()) {
            println("API key is empty")
            _uiState.value = _uiState.value.copy(
                error = "API key is required. Please configure your AI settings first."
            )
            return
        }
        
        viewModelScope.launch {
            println("Setting processing state to true")
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                error = null
            )
            
            try {
                println("Calling AI service for image analysis")
                val result = aiService.analyzeImage(bitmap, currentConfig)
                
                result.fold(
                    onSuccess = { analysis ->
                        println("Analysis successful: ${analysis.originalText.take(50)}...")
                        _uiState.value = _uiState.value.copy(
                            overallAnalysis = analysis,
                            isProcessing = false,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        println("Analysis failed: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            error = "Analysis failed: ${error.message ?: "Unknown error"}"
                        )
                    }
                )
                
            } catch (e: Exception) {
                println("Exception during analysis: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = "Image analysis failed: ${e.message ?: "Unknown error"}"
                )
            }
        }
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
        println("Running test analysis")
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
            translation = "Hello",
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
        println("Analyze with fallback called")
        val currentConfig = _uiState.value.aiConfig
        
        // If no API key, run test analysis instead
        if (currentConfig.apiKey.isEmpty()) {
            println("No API key configured, running test analysis")
            testAnalysis()
        } else {
            println("API key configured, running real analysis")
            analyzeFullImage()
        }
    }
}