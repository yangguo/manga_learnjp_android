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
    
    fun analyzeImageForInteractiveReading(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = true,
                    error = null
                )
                
                Logger.logFunctionEntry("InteractiveReadingViewModel", "analyzeImageForInteractiveReading")
                
                val currentConfig = _uiState.value.aiConfig
                
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
}