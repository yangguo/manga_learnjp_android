package com.example.manga_apk.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.manga_apk.data.*
import com.example.manga_apk.service.AIService
import com.example.manga_apk.utils.Logger
import com.example.manga_apk.utils.WakeLockManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.manga_apk.data.PreferencesRepository
import kotlinx.coroutines.flow.first

data class InteractiveReadingUiState(
    val selectedImage: Bitmap? = null,
    val identifiedSentences: List<IdentifiedSentence> = emptyList(),
    val isAnalyzing: Boolean = false,
    val error: String? = null,
    val aiConfig: AIConfig = AIConfig()
)

class InteractiveReadingViewModel(private val context: Context) : ViewModel() {
    
    private val _uiState = MutableStateFlow(InteractiveReadingUiState())
    
    val uiState: StateFlow<InteractiveReadingUiState> = _uiState.asStateFlow()
    
    private val aiService = AIService()
    private val preferencesRepository = PreferencesRepository(context)
    private val wakeLockManager = WakeLockManager(context)
    
    init {
        // Load AI configuration on initialization
        viewModelScope.launch {
            try {
                val savedConfig = preferencesRepository.aiConfigFlow.first()
                println("InteractiveReadingViewModel: Loaded saved AI config - OpenAI: ${savedConfig.openaiConfig.apiKey.trim().length} chars, Gemini: ${savedConfig.geminiConfig.apiKey.trim().length} chars")
                android.util.Log.d("MangaLearnJP", "InteractiveReadingViewModel: Loaded saved AI config - OpenAI: ${savedConfig.openaiConfig.apiKey.trim().length} chars")
                _uiState.value = _uiState.value.copy(aiConfig = savedConfig)
            } catch (e: Exception) {
                println("InteractiveReadingViewModel: Error loading saved AI config: ${e.message}")
                android.util.Log.e("MangaLearnJP", "InteractiveReadingViewModel: Error loading saved AI config", e)
            }
        }
    }
    
    fun setImage(bitmap: Bitmap?) {
        _uiState.value = _uiState.value.copy(
            selectedImage = bitmap,
            identifiedSentences = emptyList(),
            error = null
        )
        
        // Trigger analysis if both image and AI config are ready
        triggerAnalysisIfReady()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun updateAIConfig(config: AIConfig) {
        println("InteractiveReadingViewModel: updateAIConfig called - OpenAI key length: ${config.openaiConfig.apiKey.length}, Gemini key length: ${config.geminiConfig.apiKey.length}")
        android.util.Log.d("MangaLearnJP", "InteractiveReadingViewModel: updateAIConfig called - OpenAI key length: ${config.openaiConfig.apiKey.length}, Gemini key length: ${config.geminiConfig.apiKey.length}")
        
        _uiState.value = _uiState.value.copy(aiConfig = config)
        
        // Trigger analysis if we have both image and AI config ready
        triggerAnalysisIfReady()
    }
    
    private fun triggerAnalysisIfReady() {
        val currentState = _uiState.value
        val hasImage = currentState.selectedImage != null
        val configuredProviders = currentState.aiConfig.getConfiguredProviders()
        val hasConfig = configuredProviders.isNotEmpty()
        
        println("InteractiveReadingViewModel: triggerAnalysisIfReady() - hasImage=$hasImage, hasConfig=$hasConfig, providers=$configuredProviders")
        android.util.Log.d("MangaLearnJP", "InteractiveReadingViewModel: triggerAnalysisIfReady() - hasImage=$hasImage, hasConfig=$hasConfig, providers=$configuredProviders")
        
        if (hasImage && hasConfig) {
            println("InteractiveReadingViewModel: Both image and AI config ready, triggering analysis")
            android.util.Log.d("MangaLearnJP", "InteractiveReadingViewModel: Both image and AI config ready, triggering analysis")
            analyzeImageForInteractiveReading(currentState.selectedImage!!)
        } else {
            val missingRequirements = mutableListOf<String>()
            if (!hasImage) missingRequirements.add("image")
            if (!hasConfig) missingRequirements.add("AI config")
            
            println("InteractiveReadingViewModel: Waiting for: ${missingRequirements.joinToString(", ")}")
            android.util.Log.d("MangaLearnJP", "InteractiveReadingViewModel: Waiting for: ${missingRequirements.joinToString(", ")}")
        }
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
                    println("InteractiveReadingViewModel: API validation failed: $validationError")
                    android.util.Log.e("MangaLearnJP", "InteractiveReadingViewModel: API validation failed: $validationError")
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        error = validationError
                    )
                    return@launch
                }
                
                // Use the existing AIService with a specialized prompt for interactive reading
                // Use wake lock to prevent network failures during screen protection
                println("InteractiveReadingViewModel: About to acquire wake lock for analysis")
                android.util.Log.d("MangaLearnJP", "InteractiveReadingViewModel: About to acquire wake lock for analysis")
                val result = wakeLockManager.withWakeLock {
                    println("InteractiveReadingViewModel: Wake lock acquired, starting AI analysis")
                    android.util.Log.d("MangaLearnJP", "InteractiveReadingViewModel: Wake lock acquired, starting AI analysis")
                    aiService.analyzeImageForInteractiveReading(bitmap, currentConfig)
                }
                println("InteractiveReadingViewModel: Wake lock released, analysis completed")
                android.util.Log.d("MangaLearnJP", "InteractiveReadingViewModel: Wake lock released, analysis completed")
                
                if (result.isSuccess) {
                    val textAnalysis = result.getOrThrow()
                    val sentences = parseInteractiveReadingResponse(textAnalysis)
                    
                    // If no sentences were identified, show error without demo content
                    if (sentences.isEmpty()) {
                        Logger.w(Logger.Category.VIEWMODEL, "No sentences identified from AI analysis")
                        _uiState.value = _uiState.value.copy(
                            identifiedSentences = emptyList(),
                            isAnalyzing = false,
                            error = "No Japanese text was identified in the image. This could be due to:\n" +
                                    "• Image quality issues (too blurry, low resolution)\n" +
                                    "• No Japanese text visible in the image\n" +
                                    "• AI provider limitations\n\n" +
                                    "Please try uploading a clearer manga image with visible Japanese text."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            identifiedSentences = sentences,
                            isAnalyzing = false,
                            error = null
                        )
                        Logger.i(Logger.Category.VIEWMODEL, "Successfully identified ${sentences.size} sentences")
                    }
                    
                    Logger.logFunctionExit("InteractiveReadingViewModel", "analyzeImageForInteractiveReading")
                } else {
                    // Handle analysis failure without demo content
                    val exception = result.exceptionOrNull() ?: Exception("Analysis failed")
                    Logger.logError("analyzeImageForInteractiveReading", exception)
                    
                    _uiState.value = _uiState.value.copy(
                        identifiedSentences = emptyList(),
                        isAnalyzing = false,
                        error = "Analysis failed: ${exception.message}"
                    )
                }
                
            } catch (e: Exception) {
                Logger.logError("analyzeImageForInteractiveReading", e)
                
                _uiState.value = _uiState.value.copy(
                    identifiedSentences = emptyList(),
                    isAnalyzing = false,
                    error = "Analysis failed: ${e.message}"
                )
            }
        }
    }
    
    private fun parseInteractiveReadingResponse(analysis: TextAnalysis): List<IdentifiedSentence> {
        // Convert the TextAnalysis result to IdentifiedSentence objects
        // If the analysis contains identifiedSentences, use those
        if (analysis.identifiedSentences.isNotEmpty()) {
            Logger.i(Logger.Category.VIEWMODEL, "Using ${analysis.identifiedSentences.size} pre-parsed identified sentences")
            
            // Log position information for debugging
            analysis.identifiedSentences.forEachIndexed { index, sentence ->
                Logger.d(Logger.Category.VIEWMODEL, "Sentence $index: '${sentence.text}' at position (${sentence.position.x}, ${sentence.position.y}) size (${sentence.position.width}, ${sentence.position.height})")
            }
            
            // Enrich sentences that have missing vocabulary/grammar with global analysis
            return analysis.identifiedSentences.map { sentence ->
                val enrichedVocabulary = if (sentence.vocabulary.isEmpty() && analysis.vocabulary.isNotEmpty()) {
                    // Extract relevant vocabulary from global analysis
                    analysis.vocabulary.filter { vocab ->
                        sentence.text.contains(vocab.word)
                    }.take(5)
                } else {
                    sentence.vocabulary
                }
                
                val enrichedGrammar = if (sentence.grammarPatterns.isEmpty() && analysis.grammarPatterns.isNotEmpty()) {
                    // Extract relevant grammar patterns from global analysis
                    analysis.grammarPatterns.filter { pattern ->
                        sentence.text.contains(pattern.pattern) ||
                        pattern.example.contains(sentence.text) ||
                        // Check for hiragana patterns in text
                        sentence.text.matches(Regex(".*${Regex.escape(pattern.pattern)}.*"))
                    }.take(3)
                } else {
                    sentence.grammarPatterns
                }
                
                sentence.copy(
                    vocabulary = enrichedVocabulary,
                    grammarPatterns = enrichedGrammar
                )
            }
        }
        
        // Otherwise, convert sentence analyses to identified sentences
        if (analysis.sentenceAnalyses.isNotEmpty()) {
            Logger.i(Logger.Category.VIEWMODEL, "Converting ${analysis.sentenceAnalyses.size} sentence analyses to identified sentences")
            val convertedSentences = analysis.sentenceAnalyses.mapIndexed { index, sentenceAnalysis ->
                IdentifiedSentence(
                    id = index + 1,
                    text = sentenceAnalysis.originalSentence,
                    translation = sentenceAnalysis.translation,
                    position = sentenceAnalysis.position ?: generateNonOverlappingPosition(index, analysis.sentenceAnalyses.size),
                    vocabulary = sentenceAnalysis.vocabulary.ifEmpty {
                        // Extract relevant vocabulary from global analysis
                        analysis.vocabulary.filter { vocab ->
                            sentenceAnalysis.originalSentence.contains(vocab.word)
                        }.take(5)
                    },
                    grammarPatterns = analysis.grammarPatterns.filter { pattern ->
                        sentenceAnalysis.originalSentence.contains(pattern.pattern)
                    }.ifEmpty {
                        // If no specific patterns match, include general patterns
                        analysis.grammarPatterns.take(2)
                    }
                )
            }
            
            // Log position information for debugging
            convertedSentences.forEachIndexed { index, sentence ->
                Logger.d(Logger.Category.VIEWMODEL, "Converted sentence $index: '${sentence.text}' at position (${sentence.position.x}, ${sentence.position.y}) size (${sentence.position.width}, ${sentence.position.height})")
            }
            
            return convertedSentences
        }
        
        // If no sentence-specific data, but we have overall text, create a single sentence
        if (analysis.originalText.isNotEmpty()) {
            Logger.i(Logger.Category.VIEWMODEL, "Creating single sentence from overall text analysis")
            return listOf(
                IdentifiedSentence(
                    id = 1,
                    text = analysis.originalText,
                    translation = analysis.translation,
                    position = TextPosition(0.3f, 0.3f, 0.4f, 0.1f),
                    vocabulary = analysis.vocabulary,
                    grammarPatterns = analysis.grammarPatterns.filter { pattern ->
                        analysis.originalText.contains(pattern.pattern)
                    }
                )
            )
        }
        
        // Return empty list if no meaningful content
        Logger.w(Logger.Category.VIEWMODEL, "No meaningful content found in analysis result")
        return emptyList()
    }
    
    /**
     * Generates non-overlapping positions for sentence markers in a grid pattern
     * Ensures multiple sentences are visually separated on the image
     */
    private fun generateNonOverlappingPosition(index: Int, totalSentences: Int): TextPosition {
        // Create a grid layout that adapts to the number of sentences
        val cols = when {
            totalSentences <= 3 -> 3
            totalSentences <= 6 -> 3
            totalSentences <= 9 -> 3
            else -> 4
        }
        
        val rows = (totalSentences + cols - 1) / cols // Ceiling division
        
        val col = index % cols
        val row = index / cols
        
        // Calculate position with proper spacing to avoid overlap
        val marginX = 0.05f
        val marginY = 0.1f
        val spacingX = (1f - 2 * marginX) / cols
        val spacingY = (1f - 2 * marginY) / rows
        
        val x = marginX + col * spacingX + spacingX * 0.1f // Small offset within cell
        val y = marginY + row * spacingY + spacingY * 0.1f // Small offset within cell
        
        // Ensure marker size is reasonable and visible
        val width = kotlin.math.min(spacingX * 0.8f, 0.2f)
        val height = kotlin.math.min(spacingY * 0.6f, 0.08f)
        
        return TextPosition(
            x = kotlin.math.max(0f, kotlin.math.min(x, 1f - width)),
            y = kotlin.math.max(0f, kotlin.math.min(y, 1f - height)),
            width = width,
            height = height
        )
    }
    
    fun retryAnalysis() {
        viewModelScope.launch {
            try {
                // Refresh AI config from storage first
                val latestConfig = preferencesRepository.aiConfigFlow.first()
                println("InteractiveReadingViewModel: retryAnalysis() - Refreshed AI config from storage")
                println("InteractiveReadingViewModel: retryAnalysis() - OpenAI: ${latestConfig.openaiConfig.apiKey.trim().length} chars, Gemini: ${latestConfig.geminiConfig.apiKey.trim().length} chars")
                android.util.Log.d("MangaLearnJP", "InteractiveReadingViewModel: retryAnalysis() - Refreshed AI config - OpenAI: ${latestConfig.openaiConfig.apiKey.trim().length} chars")
                
                _uiState.value = _uiState.value.copy(aiConfig = latestConfig, error = null)
                
                // Trigger analysis with refreshed config
                _uiState.value.selectedImage?.let { bitmap ->
                    analyzeImageForInteractiveReading(bitmap)
                }
            } catch (e: Exception) {
                println("InteractiveReadingViewModel: Error refreshing config during retry: ${e.message}")
                android.util.Log.e("MangaLearnJP", "InteractiveReadingViewModel: Error refreshing config during retry", e)
                
                // Fallback to existing retry logic
                _uiState.value.selectedImage?.let { bitmap ->
                    analyzeImageForInteractiveReading(bitmap)
                }
            }
        }
    }
    
    private fun validateApiConfiguration(config: AIConfig): String? {
        // Check if any provider is configured
        val hasOpenAI = config.openaiConfig.apiKey.trim().isNotEmpty()
        val hasGemini = config.geminiConfig.apiKey.trim().isNotEmpty()
        val hasCustom = config.customConfig.apiKey.trim().isNotEmpty() && config.customConfig.endpoint.trim().isNotEmpty()
        
        // Debug logging for validation
        println("InteractiveReadingViewModel.validateApiConfiguration: hasOpenAI=$hasOpenAI, hasGemini=$hasGemini, hasCustom=$hasCustom")
        println("InteractiveReadingViewModel.validateApiConfiguration: Primary provider=${config.primaryProvider}")
        println("InteractiveReadingViewModel.validateApiConfiguration: OpenAI key length=${config.openaiConfig.apiKey.trim().length}")
        println("InteractiveReadingViewModel.validateApiConfiguration: Gemini key length=${config.geminiConfig.apiKey.trim().length}")
        android.util.Log.d("MangaLearnJP", "InteractiveReadingViewModel.validateApiConfiguration: hasOpenAI=$hasOpenAI, hasGemini=$hasGemini, hasCustom=$hasCustom, primary=${config.primaryProvider}")
        
        if (!hasOpenAI && !hasGemini && !hasCustom) {
            println("InteractiveReadingViewModel.validateApiConfiguration: No providers configured!")
            android.util.Log.e("MangaLearnJP", "InteractiveReadingViewModel.validateApiConfiguration: No providers configured!")
            
            return buildString {
                append("❌ No AI providers configured. Please set up at least one API key in Settings:")
                append("\n• OpenAI API key for GPT-4 Vision")
                append("\n• Google Gemini API key")
                append("\n• Custom API endpoint and key")
                append("\n\n🔍 Debug Info:")
                append("\n• Primary Provider: ${config.primaryProvider}")
                append("\n• OpenAI Key Length: ${config.openaiConfig.apiKey.length} chars")
                append("\n• Gemini Key Length: ${config.geminiConfig.apiKey.length} chars")
                append("\n• Custom Key Length: ${config.customConfig.apiKey.length} chars")
                append("\n• Custom Endpoint: ${if (config.customConfig.endpoint.isEmpty()) "Not set" else "Set"}")
            }
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
    
    fun getDebugStatus(): String {
        val config = _uiState.value.aiConfig
        return buildString {
            append("🔍 Interactive Reading Mode Debug Status:\n\n")
            append("📋 Configuration Status:\n")
            append("• Primary Provider: ${config.primaryProvider}\n")
            append("• Fallback Enabled: ${config.enableFallback}\n")
            append("• Configured Providers: ${config.getConfiguredProviders()}\n\n")
            
            append("🔑 API Keys Status:\n")
            append("• OpenAI Key: ${if (config.openaiConfig.apiKey.trim().isEmpty()) "❌ Missing" else "✅ Present (${config.openaiConfig.apiKey.trim().length} chars)"}\n")
            append("• Gemini Key: ${if (config.geminiConfig.apiKey.trim().isEmpty()) "❌ Missing" else "✅ Present (${config.geminiConfig.apiKey.trim().length} chars)"}\n")
            append("• Custom Key: ${if (config.customConfig.apiKey.trim().isEmpty()) "❌ Missing" else "✅ Present (${config.customConfig.apiKey.trim().length} chars)"}\n")
            append("• Custom Endpoint: ${if (config.customConfig.endpoint.trim().isEmpty()) "❌ Missing" else "✅ Set"}\n\n")
            
            append("🖼️ Current State:\n")
            append("• Image Loaded: ${if (_uiState.value.selectedImage != null) "✅ Yes" else "❌ No"}\n")
            append("• Currently Analyzing: ${if (_uiState.value.isAnalyzing) "⏳ Yes" else "❌ No"}\n")
            append("• Error Present: ${if (_uiState.value.error != null) "❌ Yes" else "✅ No"}\n")
            append("• Sentences Found: ${_uiState.value.identifiedSentences.size}\n\n")
            
            append("💡 Suggested Actions:\n")
            if (config.getConfiguredProviders().isEmpty()) {
                append("1. ⚙️ Go to Settings and configure at least one AI provider\n")
                append("2. 🔄 Retry analysis after configuring\n")
            } else {
                append("1. ✅ Configuration looks good\n")
                append("2. 🔄 Try retrying the analysis\n")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Ensure wake lock is released when ViewModel is destroyed
        wakeLockManager.releaseWakeLock()
    }
}