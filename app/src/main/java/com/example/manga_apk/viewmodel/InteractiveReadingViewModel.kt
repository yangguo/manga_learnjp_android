package com.example.manga_apk.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.manga_apk.data.*
import com.example.manga_apk.service.AIService
import com.example.manga_apk.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class InteractiveReadingUiState(
    val selectedImage: Bitmap? = null,
    val identifiedSentences: List<IdentifiedSentence> = emptyList(),
    val isAnalyzing: Boolean = false,
    val error: String? = null,
    val aiConfig: AIConfig = AIConfig()
)

class InteractiveReadingViewModel(private val context: Context) : ViewModel() {
    
    private val preferencesRepository = PreferencesRepository(context)
    private val _uiState = MutableStateFlow(InteractiveReadingUiState())
    
    val uiState: StateFlow<InteractiveReadingUiState> = combine(
        _uiState,
        preferencesRepository.aiConfigFlow
    ) { state, aiConfig ->
        state.copy(aiConfig = aiConfig)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InteractiveReadingUiState()
    )
    
    private val aiService = AIService()
    
    fun setImage(bitmap: Bitmap?) {
        _uiState.value = _uiState.value.copy(
            selectedImage = bitmap,
            identifiedSentences = emptyList(),
            error = null
        )
        
        // Automatically analyze the image when set
        bitmap?.let { analyzeImageForInteractiveReading(it) }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun updateAIConfig(config: AIConfig) {
        println("InteractiveReadingViewModel: updateAIConfig called - OpenAI key length: ${config.openaiConfig.apiKey.length}, Gemini key length: ${config.geminiConfig.apiKey.length}")
        android.util.Log.d("MangaLearnJP", "InteractiveReadingViewModel: updateAIConfig called - OpenAI key length: ${config.openaiConfig.apiKey.length}, Gemini key length: ${config.geminiConfig.apiKey.length}")
        
        _uiState.value = _uiState.value.copy(aiConfig = config)
    }
    
    fun analyzeImageForInteractiveReading(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = true,
                    error = null
                )
                
                Logger.logFunctionEntry("InteractiveReadingViewModel", "analyzeImageForInteractiveReading")
                
                val currentConfig = _uiState.value.aiConfig
                
                // Debug logging for config values
                println("InteractiveReadingViewModel: Current config - OpenAI key length: ${currentConfig.openaiConfig.apiKey.trim().length}, Gemini key length: ${currentConfig.geminiConfig.apiKey.trim().length}, Primary provider: ${currentConfig.primaryProvider}")
                android.util.Log.d("MangaLearnJP", "InteractiveReadingViewModel: Current config - OpenAI key length: ${currentConfig.openaiConfig.apiKey.trim().length}, Gemini key length: ${currentConfig.geminiConfig.apiKey.trim().length}, Primary provider: ${currentConfig.primaryProvider}")
                
                // Validate API configuration before making the call
                val validationError = validateApiConfiguration(currentConfig)
                if (validationError != null) {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        error = validationError
                    )
                    return@launch
                }
                
                // Use the existing AIService with a specialized prompt for interactive reading
                val result = aiService.analyzeImageForInteractiveReading(bitmap, currentConfig)
                
                val sentences = parseInteractiveReadingResponse(result)
                
                _uiState.value = _uiState.value.copy(
                    identifiedSentences = sentences,
                    isAnalyzing = false
                )
                
                Logger.logFunctionExit("InteractiveReadingViewModel", "analyzeImageForInteractiveReading")
                
            } catch (e: Exception) {
                Logger.logError("analyzeImageForInteractiveReading", e)
                
                // Fall back to demo sentences if analysis fails
                val demoSentences = generateDemoSentences()
                
                _uiState.value = _uiState.value.copy(
                    identifiedSentences = demoSentences,
                    isAnalyzing = false,
                    error = "LLM analysis failed, showing demo content: ${e.message}"
                )
            }
        }
    }
    
    private fun parseInteractiveReadingResponse(analysis: TextAnalysis): List<IdentifiedSentence> {
        // Convert the TextAnalysis result to IdentifiedSentence objects
        // If the analysis contains identifiedSentences, use those
        if (analysis.identifiedSentences.isNotEmpty()) {
            return analysis.identifiedSentences
        }
        
        // Otherwise, convert sentence analyses to identified sentences
        return analysis.sentenceAnalyses.mapIndexed { index, sentenceAnalysis ->
            IdentifiedSentence(
                id = index + 1,
                text = sentenceAnalysis.originalSentence,
                translation = sentenceAnalysis.translation,
                position = sentenceAnalysis.position ?: TextPosition(
                    x = 0.1f + (index % 3) * 0.3f,
                    y = 0.2f + (index / 3) * 0.2f,
                    width = 0.25f,
                    height = 0.06f
                ),
                vocabulary = sentenceAnalysis.vocabulary,
                grammarPatterns = analysis.grammarPatterns
            )
        }
    }
    
    private fun generateDemoSentences(): List<IdentifiedSentence> {
        return listOf(
            IdentifiedSentence(
                id = 1,
                text = "もういい出かけてくる",
                translation = "Enough, I'm going out.",
                position = TextPosition(0.3f, 0.2f, 0.2f, 0.05f),
                vocabulary = listOf(
                    VocabularyItem(
                        word = "出かける",
                        reading = "でかける",
                        meaning = "to go out",
                        partOfSpeech = "verb",
                        difficulty = 2
                    )
                ),
                grammarPatterns = listOf(
                    GrammarPattern(
                        pattern = "〜てくる",
                        explanation = "Te-form + kuru indicates going out and doing something",
                        example = "出かけてくる",
                        difficulty = "intermediate"
                    )
                )
            ),
            IdentifiedSentence(
                id = 2,
                text = "留守番してろ！",
                translation = "Stay home and watch the house!",
                position = TextPosition(0.6f, 0.4f, 0.25f, 0.06f),
                vocabulary = listOf(
                    VocabularyItem(
                        word = "留守番",
                        reading = "るすばん",
                        meaning = "house-sitting",
                        partOfSpeech = "noun",
                        difficulty = 3
                    )
                ),
                grammarPatterns = listOf(
                    GrammarPattern(
                        pattern = "〜してろ",
                        explanation = "Imperative form commanding someone to do something",
                        example = "留守番してろ",
                        difficulty = "intermediate"
                    )
                )
            )
        )
    }
    
    fun retryAnalysis() {
        _uiState.value.selectedImage?.let { bitmap ->
            analyzeImageForInteractiveReading(bitmap)
        }
    }
    
    private fun validateApiConfiguration(config: AIConfig): String? {
        // Check if any provider is configured
        val hasOpenAI = config.openaiConfig.apiKey.trim().isNotEmpty()
        val hasGemini = config.geminiConfig.apiKey.trim().isNotEmpty()
        val hasCustom = config.customConfig.apiKey.trim().isNotEmpty() && config.customConfig.endpoint.trim().isNotEmpty()
        
        // Debug logging for validation
        println("InteractiveReadingViewModel.validateApiConfiguration: hasOpenAI=$hasOpenAI, hasGemini=$hasGemini, hasCustom=$hasCustom")
        android.util.Log.d("MangaLearnJP", "InteractiveReadingViewModel.validateApiConfiguration: hasOpenAI=$hasOpenAI, hasGemini=$hasGemini, hasCustom=$hasCustom")
        
        if (!hasOpenAI && !hasGemini && !hasCustom) {
            println("InteractiveReadingViewModel.validateApiConfiguration: No providers configured!")
            android.util.Log.d("MangaLearnJP", "InteractiveReadingViewModel.validateApiConfiguration: No providers configured!")
            return "❌ No AI providers configured. Please set up at least one API key in Settings:\n" +
                    "• OpenAI API key for GPT-4 Vision\n" +
                    "• Google Gemini API key\n" +
                    "• Custom API endpoint and key"
        }
        
        // Validate primary provider
        when (config.primaryProvider) {
            AIProvider.OPENAI -> {
                if (!hasOpenAI) {
                    return "❌ OpenAI is set as primary provider but API key is missing.\n\n💡 Solution: Add your OpenAI API key in Settings or switch to a different primary provider."
                }
                val trimmedKey = config.openaiConfig.apiKey.trim()
                if (trimmedKey.length < 20) {
                    return "❌ OpenAI API key appears to be invalid (too short: ${trimmedKey.length} characters).\n\n💡 Solution: Check your OpenAI API key - it should start with 'sk-' and be much longer."
                }
                if (!trimmedKey.startsWith("sk-")) {
                    return "❌ OpenAI API key format appears incorrect (should start with 'sk-').\n\n💡 Solution: Verify you copied the correct API key from OpenAI platform."
                }
            }
            AIProvider.GEMINI -> {
                if (!hasGemini) {
                    return "❌ Gemini is set as primary provider but API key is missing.\n\n💡 Solution: Add your Google Gemini API key in Settings or switch to a different primary provider."
                }
                val trimmedKey = config.geminiConfig.apiKey.trim()
                if (trimmedKey.length < 20) {
                    return "❌ Gemini API key appears to be invalid (too short: ${trimmedKey.length} characters).\n\n💡 Solution: Check your Gemini API key from Google AI Studio."
                }
            }
            AIProvider.CUSTOM -> {
                if (!hasCustom) {
                    return "❌ Custom API is set as primary provider but API key or endpoint is missing.\n\n💡 Solution: Add your custom API key and endpoint in Settings or switch to a different primary provider."
                }
            }
        }
        
        return null // All validations passed
    }
}