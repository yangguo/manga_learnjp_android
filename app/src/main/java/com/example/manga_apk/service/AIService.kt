package com.example.manga_apk.service

import android.graphics.Bitmap
import android.util.Base64
import com.example.manga_apk.data.AIConfig
import com.example.manga_apk.data.AIProvider
import com.example.manga_apk.data.CustomAPIConfig
import com.example.manga_apk.data.GeminiConfig
import com.example.manga_apk.data.GrammarPattern
import com.example.manga_apk.data.OpenAIConfig
import com.example.manga_apk.data.SentenceAnalysis
import com.example.manga_apk.data.TextAnalysis
import com.example.manga_apk.data.VocabularyItem
import com.example.manga_apk.utils.Logger
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ConnectionPool
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.math.pow

/**
 * AIService with enhanced JSON parsing capabilities
 * 
 * JSON Handling Improvements based on Android Kotlin best practices:
 * 1. Defensive parsing with null safety validation
 * 2. Multiple parsing strategies (direct parsing + JsonObject fallback)
 * 3. Flexible field name matching (camelCase, snake_case variations)
 * 4. Safe array parsing with exception handling
 * 5. Comprehensive error logging and fallback mechanisms
 * 6. Content validation before and after parsing
 * 
 * These improvements address common JSON parsing issues:
 * - "Expected BEGIN_OBJECT but was STRING" errors
 * - Null pointer exceptions from missing fields
 * - Inconsistent field naming between API responses
 * - Malformed JSON from AI providers
 */
class AIService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .build()
    
    // Extended timeout client for image analysis operations
    private val extendedTimeoutClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)  // 3 minutes for image analysis
        .writeTimeout(120, TimeUnit.SECONDS) // 2 minutes for upload
        .retryOnConnectionFailure(true)
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .build()
    
    private val gson = Gson()
    
    // TODO: Consider upgrading to Moshi or Kotlinx.serialization for better Kotlin support
    // Moshi provides better null safety and performance (30% faster serialization)
    // Kotlinx.serialization offers compile-time safety and better integration with Kotlin
    // Reference: https://github.com/square/moshi or https://github.com/Kotlin/kotlinx.serialization
    
    // Enhanced prompts for better manga analysis
    private val mangaAnalysisPrompt = """
        You are an expert Japanese language tutor specializing in manga analysis. 
        Analyze the provided manga panel image and extract ALL Japanese text, then perform a sentence-by-sentence analysis.

        **Instructions:**
        1.  **Extract ALL Text:** Identify and extract every piece of Japanese text from the image, including dialogue, narration, and sound effects.
        2.  **Split into Sentences:** Divide the extracted text into individual sentences or logical phrases.
        3.  **Analyze Each Sentence:** For each sentence, provide:
            *   `originalSentence`: The sentence in Japanese.
            *   `translation`: The English translation of the sentence.
            *   `vocabulary`: A breakdown of key vocabulary words in that sentence.
        4.  **Provide Overall Analysis:** After analyzing all sentences, provide a combined summary including:
            *   `originalText`: All extracted Japanese text combined.
            *   `translation`: A complete English translation of all text.
            *   `grammarPatterns`: A list of grammar patterns found anywhere in the text.
            *   `context`: Any relevant cultural or contextual notes.

        **JSON Output Structure:**
        Return the analysis in a JSON object with the following structure:
        {
          "originalText": "(all Japanese text combined)",
          "translation": "(full English translation)",
          "sentenceAnalyses": [
            {
              "originalSentence": "(first Japanese sentence)",
              "translation": "(English translation of first sentence)",
              "vocabulary": [{"word": "", "reading": "", "meaning": ""}]
            },
            {
              "originalSentence": "(second Japanese sentence)",
              "translation": "(English translation of second sentence)",
              "vocabulary": [{"word": "", "reading": "", "meaning": ""}]
            }
          ],
          "grammarPatterns": [{"pattern": "", "explanation": "", "usage": ""}],
          "context": "(cultural and contextual notes)"
        }
    """.trimIndent()
    
    private val vocabularyFocusPrompt = """
        Focus specifically on vocabulary extraction and learning from this manga panel.
        IMPORTANT: Extract ALL Japanese text from EVERY element in the image (speech bubbles, sound effects, background text, etc.).
        
        For ALL Japanese words found, provide:
        - Complete text extraction from ALL sources
        - Kanji with furigana for ALL words
        - Word type (noun, verb, adjective, etc.)
        - JLPT level
        - Common usage examples
        - Related words and compounds
        - Memory aids and mnemonics
        
        Combine ALL text found into the originalText field and provide comprehensive vocabulary analysis.
    """.trimIndent()
    
    init {
        Logger.i(Logger.Category.AI_SERVICE, "Initialized with client: ${client.javaClass.simpleName}")
        Logger.i(Logger.Category.AI_SERVICE, "Standard client timeouts - Connect: ${client.connectTimeoutMillis}ms, Read: ${client.readTimeoutMillis}ms, Write: ${client.writeTimeoutMillis}ms")
        Logger.i(Logger.Category.AI_SERVICE, "Extended client timeouts - Connect: ${extendedTimeoutClient.connectTimeoutMillis}ms, Read: ${extendedTimeoutClient.readTimeoutMillis}ms, Write: ${extendedTimeoutClient.writeTimeoutMillis}ms")
        
        // Add debug logging to check if Logger is working
        println("AIService: Logger initialization test")
        android.util.Log.d("MangaLearnJP", "AIService: Direct Android Log test")
        android.util.Log.d("MangaLearnJP", "AIService: Extended timeout client configured for image analysis")
    }
    
    suspend fun testNetworkConnection(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Logger.logFunctionEntry("AIService", "testNetworkConnection")
            
            val request = Request.Builder()
                .url("https://www.google.com")
                .head() // Use HEAD request for faster response
                .build()
            
            // Use extended timeout client for image analysis
            val response = extendedTimeoutClient.newCall(request).execute()
            val isSuccessful = response.isSuccessful
            
            Logger.i(Logger.Category.NETWORK, "Network test result: $isSuccessful (status: ${response.code})")
            response.close()
            
            isSuccessful
        } catch (e: Exception) {
            Logger.logError("testNetworkConnection", e)
            false
        }
    }
    
    suspend fun analyzeImage(
        bitmap: Bitmap,
        config: AIConfig
    ): Result<TextAnalysis> = withContext(Dispatchers.IO) {
        try {
            // Add debug prints to trace execution
            println("AIService: Starting analyzeImage")
            android.util.Log.d("MangaLearnJP", "AIService: Starting analyzeImage with bitmap ${bitmap.width}x${bitmap.height}")
            
            Logger.logFunctionEntry("AIService", "analyzeImage", mapOf(
                "bitmapSize" to "${bitmap.width}x${bitmap.height}",
                "primaryProvider" to config.primaryProvider.toString(),
                "fallbackEnabled" to config.enableFallback.toString()
            ))
            
            val providersToTry = config.getConfiguredProviders()
            Logger.i(Logger.Category.AI_SERVICE, "Configured providers to try: $providersToTry")
            println("AIService: Providers to try: $providersToTry")
            
            if (providersToTry.isEmpty()) {
                val errorMsg = "No API providers are configured"
                Logger.logError("analyzeImage", errorMsg)
                println("AIService: ERROR - $errorMsg")
                return@withContext Result.failure(
                    IllegalArgumentException(errorMsg)
                )
            }
            
            var lastException: Exception? = null
            
            for (provider in providersToTry) {
                Logger.i(Logger.Category.AI_SERVICE, "Attempting analysis with provider: $provider")
                println("AIService: Attempting analysis with provider: $provider")
                android.util.Log.d("MangaLearnJP", "AIService: Attempting analysis with provider: $provider")
                
                val result = when (provider) {
                    AIProvider.OPENAI -> {
                        Logger.i(Logger.Category.AI_SERVICE, "Using OpenAI provider")
                        println("AIService: Using OpenAI provider")
                        analyzeWithOpenAI(bitmap, config.openaiConfig)
                    }
                    AIProvider.GEMINI -> {
                        Logger.i(Logger.Category.AI_SERVICE, "Using Gemini provider")
                        println("AIService: Using Gemini provider")
                        analyzeWithGemini(bitmap, config.geminiConfig)
                    }
                    AIProvider.CUSTOM -> {
                        Logger.i(Logger.Category.AI_SERVICE, "Using Custom provider")
                        println("AIService: Using Custom provider")
                        analyzeWithCustomAPI(bitmap, config.customConfig)
                    }
                }
                
                if (result.isSuccess) {
                    Logger.i(Logger.Category.AI_SERVICE, "Analysis successful with provider: $provider")
                    println("AIService: Analysis successful with provider: $provider")
                    return@withContext result
                } else {
                    lastException = result.exceptionOrNull() as? Exception
                    Logger.logError("analyzeImage", lastException ?: Exception("Analysis failed with provider: $provider"))
                    println("AIService: Analysis failed with provider: $provider - ${lastException?.message}")
                    
                    // If fallback is disabled and this is the primary provider, return the failure
                    if (!config.enableFallback && provider == config.primaryProvider) {
                        Logger.i(Logger.Category.AI_SERVICE, "Fallback disabled, returning failure from primary provider")
                        println("AIService: Fallback disabled, returning failure from primary provider")
                        return@withContext result
                    }
                }
            }
            
            Logger.logError("analyzeImage", lastException ?: Exception("All providers failed"))
            println("AIService: All providers failed - ${lastException?.message}")
            android.util.Log.e("MangaLearnJP", "AIService: All providers failed", lastException)
            Result.failure(lastException ?: Exception("All configured providers failed"))
        } catch (e: Exception) {
            Logger.logError("analyzeImage", e)
            println("AIService: Exception in analyzeImage - ${e.message}")
            android.util.Log.e("MangaLearnJP", "AIService: Exception in analyzeImage", e)
            Result.failure(e)
        }
    }
    
    suspend fun analyzeImageEnhanced(
        bitmap: Bitmap,
        config: AIConfig,
        analysisType: AnalysisType = AnalysisType.COMPREHENSIVE
    ): Result<TextAnalysis> = withContext(Dispatchers.IO) {
        try {
            Logger.i(Logger.Category.AI_SERVICE, "Starting enhanced analysis with type: ${analysisType.name}")
            
            val prompt = when (analysisType) {
                AnalysisType.COMPREHENSIVE -> mangaAnalysisPrompt
                AnalysisType.VOCABULARY_FOCUS -> vocabularyFocusPrompt
                AnalysisType.QUICK_TRANSLATION -> "Extract ALL Japanese text from EVERY element in this manga panel (speech bubbles, sound effects, background text, etc.) and provide quick translation. Combine ALL text found into a single comprehensive result."
            }
            
            // Use the enhanced analysis with custom prompt
            when (config.primaryProvider) {
                AIProvider.OPENAI -> {
                    analyzeWithOpenAI(bitmap, config.openaiConfig)
                }
                AIProvider.GEMINI -> {
                    analyzeWithGemini(bitmap, config.geminiConfig)
                }
                else -> analyzeImage(bitmap, config)
            }
        } catch (e: Exception) {
            Logger.logError("analyzeImageEnhanced", e)
            // Fallback to basic analysis
            analyzeImage(bitmap, config)
        }
    }
    
    enum class AnalysisType {
        COMPREHENSIVE,
        VOCABULARY_FOCUS,
        QUICK_TRANSLATION
    }
    
    private suspend fun analyzeWithOpenAI(
        bitmap: Bitmap,
        config: OpenAIConfig
    ): Result<TextAnalysis> {
        println("AIService: Starting analyzeWithOpenAI")
        android.util.Log.d("MangaLearnJP", "AIService: analyzeWithOpenAI - API key length: ${config.apiKey.length}")
        
        Logger.logFunctionEntry("AIService", "analyzeWithOpenAI", mapOf(
            "bitmapSize" to "${bitmap.width}x${bitmap.height}",
            "visionModel" to config.visionModel
        ))
        
        if (config.apiKey.isEmpty()) {
            val errorMsg = "OpenAI API key is not configured"
            println("AIService: ERROR - $errorMsg")
            android.util.Log.e("MangaLearnJP", "AIService: $errorMsg")
            return Result.failure(IllegalArgumentException(errorMsg))
        }
        
        val base64Image = bitmapToBase64(bitmap)
        Logger.i(Logger.Category.IMAGE, "Image converted to base64, length: ${base64Image.length}")
        
        // Use the visionModel for OpenAI provider
        val modelToUse = config.visionModel
        Logger.i(Logger.Category.AI_SERVICE, "Using OpenAI model: $modelToUse")
        
        val requestBody = JsonObject().apply {
            addProperty("model", modelToUse)
            add("messages", gson.toJsonTree(listOf(
                mapOf(
                    "role" to "user",
                    "content" to listOf(
                        mapOf("type" to "text", "text" to mangaAnalysisPrompt),
                        mapOf(
                            "type" to "image_url",
                            "image_url" to mapOf("url" to "data:image/jpeg;base64,$base64Image")
                        )
                    )
                )
            )))
            addProperty("max_tokens", 4000)
            addProperty("temperature", 0.3)
        }
        
        Logger.i(Logger.Category.AI_SERVICE, "Request body prepared")
        Logger.i(Logger.Category.AI_SERVICE, "Using model: $modelToUse")
        
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        Logger.i(Logger.Category.NETWORK, "Making API request to OpenAI")
        
        return try {
            Logger.i(Logger.Category.NETWORK, "Making API request to OpenAI")
            Logger.i(Logger.Category.NETWORK, "Request URL: https://api.openai.com/v1/chat/completions")
            Logger.i(Logger.Category.NETWORK, "Request headers: Authorization=Bearer [REDACTED], Content-Type=application/json")
            
            println("AIService: Making HTTP request to OpenAI API")
            android.util.Log.d("MangaLearnJP", "AIService: Making HTTP request to OpenAI API")
            
            // Use extended timeout client for image analysis
            val response = extendedTimeoutClient.newCall(request).execute()
            Logger.i(Logger.Category.NETWORK, "Response received, status: ${response.code}")
            Logger.i(Logger.Category.NETWORK, "Response headers: ${response.headers}")
            
            println("AIService: Response received, status: ${response.code}")
            android.util.Log.d("MangaLearnJP", "AIService: Response received, status: ${response.code}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Logger.i(Logger.Category.NETWORK, "Response body length: ${responseBody?.length ?: 0}")
                if (responseBody != null && responseBody.length > 100) {
                    Logger.i(Logger.Category.NETWORK, "Response body preview: ${responseBody.take(200)}...")
                } else {
                    Logger.i(Logger.Category.NETWORK, "Full response body: $responseBody")
                }
                
                println("AIService: Successful response, parsing...")
                android.util.Log.d("MangaLearnJP", "AIService: Successful response, parsing...")
                
                parseOpenAIResponse(responseBody)
            } else {
                val errorBody = response.body?.string()
                Logger.logError("analyzeWithOpenAI", "API call failed with status ${response.code}")
                Logger.logError("analyzeWithOpenAI", "Error body: $errorBody")
                
                println("AIService: API call failed with status ${response.code}")
                println("AIService: Error body: $errorBody")
                android.util.Log.e("MangaLearnJP", "AIService: API call failed with status ${response.code}")
                android.util.Log.e("MangaLearnJP", "AIService: Error body: $errorBody")
                
                // Provide more specific error messages based on status code
                val errorMessage = when (response.code) {
                    401 -> "❌ OpenAI API Authentication Failed (401)\n" +
                           "• Check if your API key is correct\n" +
                           "• Verify the API key has proper permissions\n" +
                           "• Make sure the API key is not expired"
                    403 -> "❌ OpenAI API Access Forbidden (403)\n" +
                           "• Your API key may not have access to GPT-4 Vision\n" +
                           "• Check your OpenAI account billing status\n" +
                           "• Verify your usage limits"
                    429 -> "❌ OpenAI API Rate Limit Exceeded (429)\n" +
                           "• You've exceeded your API rate limits\n" +
                           "• Wait a moment and try again\n" +
                           "• Consider upgrading your OpenAI plan"
                    500, 502, 503, 504 -> "❌ OpenAI Server Error (${response.code})\n" +
                                          "• OpenAI servers are experiencing issues\n" +
                                          "• Try again in a few minutes\n" +
                                          "• Check OpenAI status page"
                    else -> "❌ OpenAI API Error (${response.code})\n" +
                            "• Error details: $errorBody\n" +
                            "• Check your API configuration"
                }
                
                Result.failure(IOException(errorMessage))
            }
        } catch (e: SocketTimeoutException) {
            Logger.logError("analyzeWithOpenAI", e)
            println("AIService: Socket timeout exception: ${e.message}")
            android.util.Log.e("MangaLearnJP", "AIService: Socket timeout exception", e)
            val errorMessage = "❌ Request Timeout\n" +
                              "• The request to OpenAI took too long\n" +
                              "• Check your internet connection\n" +
                              "• Try again with a smaller image\n" +
                              "• OpenAI servers might be slow"
            Result.failure(IOException(errorMessage))
        } catch (e: ConnectException) {
            Logger.logError("analyzeWithOpenAI", e)
            println("AIService: Connection exception: ${e.message}")
            android.util.Log.e("MangaLearnJP", "AIService: Connection exception", e)
            val errorMessage = "❌ Connection Failed\n" +
                              "• Cannot connect to OpenAI servers\n" +
                              "• Check your internet connection\n" +
                              "• Verify you're not behind a firewall\n" +
                              "• Try again in a few moments"
            Result.failure(IOException(errorMessage))
        } catch (e: UnknownHostException) {
            Logger.logError("analyzeWithOpenAI", e)
            println("AIService: Unknown host exception: ${e.message}")
            android.util.Log.e("MangaLearnJP", "AIService: Unknown host exception", e)
            val errorMessage = "❌ Network Error\n" +
                              "• Cannot resolve OpenAI server address\n" +
                              "• Check your internet connection\n" +
                              "• Verify DNS settings\n" +
                              "• Try switching networks (WiFi/Mobile)"
            Result.failure(IOException(errorMessage))
        } catch (e: IOException) {
            Logger.logError("analyzeWithOpenAI", e)
            println("AIService: IO exception: ${e.message}")
            android.util.Log.e("MangaLearnJP", "AIService: IO exception", e)
            val errorMessage = "❌ Network I/O Error\n" +
                              "• ${e.message}\n" +
                              "• Check your internet connection\n" +
                              "• Try again in a few moments"
            Result.failure(IOException(errorMessage))
        } catch (e: Exception) {
            Logger.logError("analyzeWithOpenAI", e)
            println("AIService: General exception: ${e.message}")
            android.util.Log.e("MangaLearnJP", "AIService: General exception", e)
            val errorMessage = "❌ Unexpected Error\n" +
                              "• ${e.message}\n" +
                              "• Check the logs for more details\n" +
                              "• Try restarting the app"
            Result.failure(IOException(errorMessage))
        }
    }
    
    private suspend fun analyzeWithGemini(
        bitmap: Bitmap,
        config: GeminiConfig
    ): Result<TextAnalysis> {
        val base64Image = bitmapToBase64(bitmap)
        
        val requestBody = JsonObject().apply {
            add("contents", gson.toJsonTree(listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to mangaAnalysisPrompt),
                        mapOf(
                            "inline_data" to mapOf(
                                "mime_type" to "image/jpeg",
                                "data" to base64Image
                            )
                        )
                    )
                )
            )))
            add("generationConfig", gson.toJsonTree(mapOf(
                "temperature" to 0.3,
                "topK" to 32,
                "topP" to 1,
                "maxOutputTokens" to 2048
            )))
            add("safetySettings", gson.toJsonTree(listOf(
                mapOf(
                    "category" to "HARM_CATEGORY_HARASSMENT",
                    "threshold" to "BLOCK_MEDIUM_AND_ABOVE"
                ),
                mapOf(
                    "category" to "HARM_CATEGORY_HATE_SPEECH",
                    "threshold" to "BLOCK_MEDIUM_AND_ABOVE"
                )
            )))
        }
        
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/${config.model}:generateContent?key=${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        return try {
            // Use extended timeout client for image analysis
            val response = extendedTimeoutClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                parseGeminiResponse(responseBody)
            } else {
                val errorBody = response.body?.string()
                Logger.logError("analyzeWithGemini", "Gemini API error: ${response.code} - $errorBody")
                Result.failure(IOException("Gemini API call failed: ${response.code} - $errorBody"))
            }
        } catch (e: Exception) {
            Logger.logError("analyzeWithGemini", e)
            Result.failure(IOException("Network error: ${e.message}", e))
        }
    }
    
    private suspend fun analyzeWithCustomAPI(
        bitmap: Bitmap,
        config: CustomAPIConfig
    ): Result<TextAnalysis> {
        println("AIService: Starting analyzeWithCustomAPI")
        println("AIService: Bitmap info - Size: ${bitmap.width}x${bitmap.height}, Config: ${bitmap.config}")
        android.util.Log.d("MangaLearnJP", "AIService: Bitmap info - Size: ${bitmap.width}x${bitmap.height}, Config: ${bitmap.config}")
        
        val base64Image = bitmapToBase64(bitmap)
        if (base64Image.isEmpty()) {
            val errorMsg = "Failed to convert bitmap to base64"
            println("AIService: ERROR - $errorMsg")
            android.util.Log.e("MangaLearnJP", "AIService: $errorMsg")
            return Result.failure(IllegalArgumentException(errorMsg))
        }
        
        // Use the model from custom config, with fallback
        val modelToUse = if (config.model.trim().isEmpty()) {
            "gpt-4-vision-preview" // Fallback model for vision tasks
        } else {
            config.model.trim()
        }
        
        println("AIService: Using custom API model: '$modelToUse'")
        println("AIService: Custom API endpoint: '${config.endpoint}'")
        println("AIService: Custom API key length: ${config.apiKey.length}")
        android.util.Log.d("MangaLearnJP", "AIService: Custom API - Endpoint: '${config.endpoint}', Model: '$modelToUse', Key length: ${config.apiKey.length}")
        
        // Validate model
        if (modelToUse.isEmpty()) {
            val errorMsg = "Custom API model is empty - please specify a model in Settings"
            println("AIService: ERROR - $errorMsg")
            android.util.Log.e("MangaLearnJP", "AIService: $errorMsg")
            return Result.failure(IllegalArgumentException(errorMsg))
        }
        
        // Validate endpoint URL
        if (config.endpoint.trim().isEmpty()) {
            val errorMsg = "Custom API endpoint is empty"
            println("AIService: ERROR - $errorMsg")
            android.util.Log.e("MangaLearnJP", "AIService: $errorMsg")
            return Result.failure(IllegalArgumentException(errorMsg))
        }
        
        if (!config.endpoint.startsWith("http://") && !config.endpoint.startsWith("https://")) {
            val errorMsg = "Custom API endpoint must start with http:// or https://. Current: '${config.endpoint}'"
            println("AIService: ERROR - $errorMsg")
            android.util.Log.e("MangaLearnJP", "AIService: $errorMsg")
            return Result.failure(IllegalArgumentException(errorMsg))
        }
        
        // Auto-append correct chat completions path for OpenAI-compatible APIs if not present
        val finalEndpoint = if (config.endpoint.contains("/chat/completions")) {
            // Already has chat/completions path, use as-is
            config.endpoint.trim()
        } else {
            val baseUrl = config.endpoint.trim().removeSuffix("/")
            // For Ark API (doubao), use /chat/completions directly
            // For standard OpenAI APIs, use /v1/chat/completions
            if (baseUrl.contains("ark.cn-beijing.volces.com") || baseUrl.contains("/api/v3")) {
                "$baseUrl/chat/completions"
            } else {
                "$baseUrl/v1/chat/completions"
            }
        }
        
        println("AIService: Final endpoint URL: '$finalEndpoint'")
        android.util.Log.d("MangaLearnJP", "AIService: Final endpoint URL: '$finalEndpoint'")
        
        // Validate API key
        if (config.apiKey.trim().isEmpty()) {
            val errorMsg = "Custom API key is empty - please add your API key in Settings"
            println("AIService: ERROR - $errorMsg")
            android.util.Log.e("MangaLearnJP", "AIService: $errorMsg")
            return Result.failure(IllegalArgumentException(errorMsg))
        }
        
        val requestBody = JsonObject().apply {
            addProperty("model", modelToUse)
            add("messages", gson.toJsonTree(listOf(
                mapOf(
                    "role" to "user",
                    "content" to listOf(
                        mapOf("type" to "text", "text" to MANGA_ANALYSIS_PROMPT),
                        mapOf(
                            "type" to "image_url",
                            "image_url" to mapOf("url" to "data:image/jpeg;base64,$base64Image")
                        )
                    )
                )
            )))
            addProperty("max_tokens", 4000)
        }
        
        // Log the request body structure (without the full base64 image for readability)
        val requestBodyForLogging = JsonObject().apply {
            addProperty("model", modelToUse)
            add("messages", gson.toJsonTree(listOf(
                mapOf(
                    "role" to "user",
                    "content" to listOf(
                        mapOf("type" to "text", "text" to MANGA_ANALYSIS_PROMPT.take(100) + "..."),
                        mapOf(
                            "type" to "image_url",
                            "image_url" to mapOf("url" to "data:image/jpeg;base64,[BASE64_IMAGE_${base64Image.length}_CHARS]")
                        )
                    )
                )
            )))
            addProperty("max_tokens", 4000)
        }
        
        println("AIService: Request body structure: ${gson.toJson(requestBodyForLogging)}")
        android.util.Log.d("MangaLearnJP", "AIService: Request body structure: ${gson.toJson(requestBodyForLogging)}")
        android.util.Log.d("MangaLearnJP", "AIService: Base64 image length: ${base64Image.length} characters")
        
        val requestBuilder = Request.Builder()
            .url(finalEndpoint)
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
        
        if (config.apiKey.isNotEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer ${config.apiKey}")
            println("AIService: Added Authorization header with key")
            android.util.Log.d("MangaLearnJP", "AIService: Added Authorization header")
        } else {
            println("AIService: WARNING - No API key provided")
            android.util.Log.w("MangaLearnJP", "AIService: WARNING - No API key provided")
        }
        
        return try {
            println("AIService: Making request to: ${requestBuilder.build().url}")
            android.util.Log.d("MangaLearnJP", "AIService: Making request to: ${requestBuilder.build().url}")
            
            // Use extended timeout client for image analysis with retry logic
            val response = executeWithRetry(requestBuilder.build(), maxRetries = 2)
            
            println("AIService: Response code: ${response.code}")
            println("AIService: Response message: ${response.message}")
            android.util.Log.d("MangaLearnJP", "AIService: Response code: ${response.code}, message: ${response.message}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    println("AIService: Successful response received, length: ${responseBody.length}")
                    println("AIService: Response preview: ${responseBody.take(200)}...")
                    android.util.Log.d("MangaLearnJP", "AIService: Response length: ${responseBody.length}")
                    android.util.Log.d("MangaLearnJP", "AIService: Response preview: ${responseBody.take(200)}...")
                    
                    parseOpenAIResponse(responseBody) // Use OpenAI format parser
                } else {
                    val errorMsg = "Empty response body"
                    println("AIService: ERROR - $errorMsg")
                    android.util.Log.e("MangaLearnJP", "AIService: $errorMsg")
                    Result.failure(IOException(errorMsg))
                }
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                val detailedErrorMsg = when (response.code) {
                    404 -> {
                        "Custom API endpoint not found (404). Please check:\n" +
                        "• Is your API server running and accessible?\n" +
                        "• Is the endpoint URL correct for your API provider?\n" +
                        "• Attempted endpoint: '$finalEndpoint'\n" +
                        "• Original endpoint: '${config.endpoint}'\n" +
                        "• For Ark/Doubao APIs: use base URL like 'https://ark.cn-beijing.volces.com/api/v3'\n" +
                        "• For OpenAI APIs: use 'https://api.openai.com'\n" +
                        "• Try enabling fallback to OpenAI/Gemini in Settings"
                    }
                    401, 403 -> {
                        "Custom API authentication failed (${response.code}). Please check:\n" +
                        "• Is your API key correct?\n" +
                        "• Does your API server require authentication?\n" +
                        "• Current key length: ${config.apiKey.length} characters"
                    }
                    500, 502, 503 -> {
                        "Custom API server error (${response.code}). The API server may be:\n" +
                        "• Temporarily unavailable\n" +
                        "• Overloaded\n" +
                        "• Misconfigured\n" +
                        "• Try again later or enable fallback mode"
                    }
                    else -> {
                        "Custom API call failed: ${response.code} - $errorBody"
                    }
                }
                
                println("AIService: ERROR - $detailedErrorMsg")
                println("AIService: Request URL was: ${requestBuilder.build().url}")
                println("AIService: Request headers: ${requestBuilder.build().headers}")
                android.util.Log.e("MangaLearnJP", "AIService: $detailedErrorMsg")
                android.util.Log.e("MangaLearnJP", "AIService: Request URL was: ${requestBuilder.build().url}")
                Result.failure(IOException(detailedErrorMsg))
            }
        } catch (e: SocketException) {
            val connectionType = when {
                e.message?.contains("connection abort") == true -> "connection abort"
                e.message?.contains("connection reset") == true -> "connection reset"
                e.message?.contains("broken pipe") == true -> "broken pipe"
                else -> "socket error"
            }
            val errorMsg = "Network ${connectionType} in custom API call: ${e.message}. " +
                    "This may indicate network instability or server issues. Try again later."
            println("AIService: ERROR - $errorMsg")
            android.util.Log.e("MangaLearnJP", "AIService: $errorMsg", e)
            Result.failure(IOException("Network error: ${connectionType}", e))
        } catch (e: SocketTimeoutException) {
            val errorMsg = "Timeout in custom API call: ${e.message}. " +
                    "The request took too long to complete. Try again later."
            println("AIService: ERROR - $errorMsg")
            android.util.Log.e("MangaLearnJP", "AIService: $errorMsg", e)
            Result.failure(IOException("Network error: timeout", e))
        } catch (e: Exception) {
            val errorMsg = "Exception in custom API call: ${e.message}"
            println("AIService: ERROR - $errorMsg")
            android.util.Log.e("MangaLearnJP", "AIService: $errorMsg", e)
            Result.failure(IOException("Network error: ${e.message}", e))
        }
    }
    
    /**
     * Execute HTTP request with retry logic and extended timeouts for image analysis
     */
    private fun executeWithRetry(request: Request, maxRetries: Int = 2): Response {
        var lastException: Exception? = null
        
        for (attempt in 0..maxRetries) {
            try {
                if (attempt > 0) {
                                        val delayMs = (1000 * 2.0.pow(attempt)).toLong() // Exponential backoff
                    println("AIService: Retry attempt $attempt after ${delayMs}ms delay")
                    android.util.Log.d("MangaLearnJP", "AIService: Retry attempt $attempt after ${delayMs}ms delay")
                    Thread.sleep(delayMs)
                }
                
                println("AIService: Executing request (attempt ${attempt + 1}/${maxRetries + 1}) with extended timeout")
                android.util.Log.d("MangaLearnJP", "AIService: Executing request (attempt ${attempt + 1}/${maxRetries + 1})")
                
                return extendedTimeoutClient.newCall(request).execute()
                
            } catch (e: java.net.SocketTimeoutException) {
                lastException = e
                val timeoutType = when {
                    e.message?.contains("connect") == true -> "connection"
                    e.message?.contains("read") == true -> "read"
                    else -> "network"
                }
                
                println("AIService: ${timeoutType.capitalize()} timeout on attempt ${attempt + 1}: ${e.message}")
                android.util.Log.w("MangaLearnJP", "AIService: ${timeoutType.capitalize()} timeout on attempt ${attempt + 1}: ${e.message}")
                
                if (attempt == maxRetries) {
                    val errorMsg = "Request failed after ${maxRetries + 1} attempts due to ${timeoutType} timeout. " +
                            "This may indicate: \n" +
                            "• The API server is overloaded or slow\n" +
                            "• Network connectivity issues\n" +
                            "• Large image processing taking longer than expected\n" +
                            "• Try again later or check your network connection"
                    throw IOException(errorMsg, e)
                }
            } catch (e: java.net.SocketException) {
                lastException = e
                val connectionType = when {
                    e.message?.contains("connection abort") == true -> "connection abort"
                    e.message?.contains("connection reset") == true -> "connection reset"
                    e.message?.contains("broken pipe") == true -> "broken pipe"
                    else -> "socket error"
                }
                
                println("AIService: Socket ${connectionType} on attempt ${attempt + 1}: ${e.message}")
                android.util.Log.w("MangaLearnJP", "AIService: Socket ${connectionType} on attempt ${attempt + 1}: ${e.message}")
                
                if (attempt == maxRetries) {
                    val errorMsg = "Request failed after ${maxRetries + 1} attempts due to ${connectionType}. " +
                            "This may indicate: \n" +
                            "• Unstable network connection\n" +
                            "• API server connection issues\n" +
                            "• Firewall or proxy interference\n" +
                            "• Try again later or check your network settings"
                    throw IOException(errorMsg, e)
                }
            } catch (e: Exception) {
                lastException = e
                println("AIService: Network error on attempt ${attempt + 1}: ${e.message}")
                android.util.Log.w("MangaLearnJP", "AIService: Network error on attempt ${attempt + 1}: ${e.message}")
                
                if (attempt == maxRetries) {
                    throw e
                }
            }
        }
        
        // This should never be reached, but just in case
        throw lastException ?: IOException("Request failed after $maxRetries retries")
    }
    
    private fun parseOpenAIResponse(responseBody: String?): Result<TextAnalysis> {
        return try {
            Logger.logFunctionEntry("AIService", "parseOpenAIResponse", mapOf(
                "responseLength" to (responseBody?.length ?: 0).toString()
            ))
            
            if (responseBody.isNullOrEmpty()) {
                Logger.logError("parseOpenAIResponse", "Response body is null or empty")
                return Result.failure(Exception("Empty response body"))
            }
            
            val jsonResponse = try {
                gson.fromJson(responseBody, JsonObject::class.java)
            } catch (e: JsonSyntaxException) {
                Logger.logError("parseOpenAIResponse", "Invalid JSON response: ${e.message}")
                Logger.d(Logger.Category.AI_SERVICE, "Response body preview: ${responseBody.take(500)}")
                return Result.failure(Exception("Invalid JSON response from OpenAI: ${e.message}"))
            }
            
            val choices = jsonResponse.getAsJsonArray("choices")
            if (choices == null || choices.size() == 0) {
                Logger.logError("parseOpenAIResponse", "No choices in response")
                return Result.failure(Exception("No choices in OpenAI response"))
            }
            
            val firstChoice = choices[0].asJsonObject
            val message = firstChoice.getAsJsonObject("message")
            val content = message.get("content").asString
            
            Logger.i(Logger.Category.AI_SERVICE, "Extracted content from OpenAI response, length: ${content.length}")
            
            parseAnalysisContent(content)
            
        } catch (e: Exception) {
            Logger.logError("parseOpenAIResponse", e)
            Result.failure(e)
        }
    }
    
    private fun parseOpenAIResponseEnhanced(responseBody: String): TextAnalysis {
        try {
            val jsonResponse = try {
                gson.fromJson(responseBody, JsonObject::class.java)
            } catch (e: JsonSyntaxException) {
                Logger.logError("parseOpenAIResponseEnhanced", "Invalid JSON response: ${e.message}")
                Logger.d(Logger.Category.AI_SERVICE, "Response body preview: ${responseBody.take(500)}")
                return createFallbackAnalysis("Invalid JSON response from OpenAI: ${e.message}")
            }
            
            // Check for errors first
            if (jsonResponse.has("error")) {
                val error = jsonResponse.getAsJsonObject("error")
                val errorMessage = error.get("message").asString
                Logger.logError("parseOpenAIResponseEnhanced", "OpenAI API error: $errorMessage")
                return createFallbackAnalysis("API Error: $errorMessage")
            }
            
            val choices = jsonResponse.getAsJsonArray("choices")
            if (choices.size() == 0) {
                return createFallbackAnalysis("No choices in response")
            }
            
            val content = choices[0].asJsonObject
                .getAsJsonObject("message")
                .get("content").asString
            
            // Enhanced parsing with better error handling
            return parseAnalysisContentEnhanced(content)
        } catch (e: Exception) {
            Logger.logError("parseOpenAIResponseEnhanced", e)
            return createFallbackAnalysis("Failed to parse enhanced response: ${e.message}")
        }
    }
    
    private fun parseGeminiResponse(responseBody: String?): Result<TextAnalysis> {
        return try {
            if (responseBody == null) {
                return Result.failure(IOException("Empty response body"))
            }
            
            val jsonResponse = try {
                gson.fromJson(responseBody, JsonObject::class.java)
            } catch (e: JsonSyntaxException) {
                Logger.logError("parseGeminiResponse", "Invalid JSON response: ${e.message}")
                Logger.d(Logger.Category.AI_SERVICE, "Response body preview: ${responseBody.take(500)}")
                return Result.failure(IOException("Invalid JSON response from Gemini: ${e.message}"))
            }
            
            if (jsonResponse.has("error")) {
                val error = jsonResponse.getAsJsonObject("error")
                val message = error.get("message").asString
                return Result.failure(IOException("Gemini API error: $message"))
            }
            
            val candidates = jsonResponse.getAsJsonArray("candidates")
            if (candidates.size() == 0) {
                return Result.failure(IOException("No candidates in response"))
            }
            
            val content = candidates[0].asJsonObject
                .getAsJsonObject("content")
                .getAsJsonArray("parts")[0].asJsonObject
                .get("text").asString
            
            parseAnalysisContent(content)
        } catch (e: Exception) {
            Logger.logError("parseGeminiResponse", e)
            Result.failure(IOException("Failed to parse Gemini response: ${e.message}", e))
        }
    }
    
    private fun parseGeminiResponseEnhanced(responseBody: String): TextAnalysis {
        try {
            val jsonResponse = try {
                gson.fromJson(responseBody, JsonObject::class.java)
            } catch (e: JsonSyntaxException) {
                Logger.logError("parseGeminiResponseEnhanced", "Invalid JSON response: ${e.message}")
                Logger.d(Logger.Category.AI_SERVICE, "Response body preview: ${responseBody.take(500)}")
                return createFallbackAnalysis("Invalid JSON response from Gemini: ${e.message}")
            }
            
            // Enhanced error checking
            if (jsonResponse.has("error")) {
                val error = jsonResponse.getAsJsonObject("error")
                val message = error.get("message").asString
                val code = if (error.has("code")) error.get("code").asInt else 0
                Logger.logError("parseGeminiResponseEnhanced", "Gemini API error [$code]: $message")
                return createFallbackAnalysis("Gemini API error: $message")
            }
            
            val candidates = jsonResponse.getAsJsonArray("candidates")
            if (candidates.size() == 0) {
                return createFallbackAnalysis("No candidates in Gemini response")
            }
            
            val candidate = candidates[0].asJsonObject
            
            // Check for content filtering
            if (candidate.has("finishReason")) {
                val finishReason = candidate.get("finishReason").asString
                if (finishReason != "STOP") {
                    Logger.w(Logger.Category.AI_SERVICE, "Gemini response finished with reason: $finishReason")
                    if (finishReason == "SAFETY") {
                        return createFallbackAnalysis("Content was filtered for safety reasons")
                    }
                }
            }
            
            val content = candidate.getAsJsonObject("content")
                .getAsJsonArray("parts")[0].asJsonObject
                .get("text").asString
            
            // Use enhanced parsing
            return parseAnalysisContentEnhanced(content)
        } catch (e: Exception) {
            Logger.logError("parseGeminiResponseEnhanced", e)
            return createFallbackAnalysis("Failed to parse enhanced Gemini response: ${e.message}")
        }
    }
    
    private fun parseAnalysisContent(content: String): Result<TextAnalysis> {
        return try {
            Logger.logFunctionEntry("AIService", "parseAnalysisContent", mapOf(
                "contentLength" to content.length.toString()
            ))
            
            println("AIService: Parsing analysis content, length: ${content.length}")
            println("AIService: Content preview: ${content.take(300)}...")
            android.util.Log.d("MangaLearnJP", "AIService: Parsing content length: ${content.length}")
            android.util.Log.d("MangaLearnJP", "AIService: Content preview: ${content.take(300)}...")
            
            // Enhanced JSON parsing with better validation
            val cleanedContent = cleanJsonContent(content)
            
            // Check if content looks like JSON
            if (isValidJsonStructure(cleanedContent)) {
                val analysis = parseJsonWithValidation(cleanedContent)
                if (analysis != null) {
                    println("AIService: Successfully parsed JSON analysis - Translation: ${analysis.translation.take(100)}...")
                    android.util.Log.d("MangaLearnJP", "AIService: Successfully parsed JSON analysis")
                    Logger.i(Logger.Category.AI_SERVICE, "Successfully parsed JSON analysis with enhanced validation")
                    return Result.success(analysis)
                }
                Logger.w(Logger.Category.AI_SERVICE, "Enhanced JSON parsing failed, content may not be valid JSON")
                println("AIService: Enhanced JSON parsing failed")
            }
            
            // If JSON parsing fails, try enhanced fallback parsing
            Logger.w(Logger.Category.AI_SERVICE, "JSON parsing failed, attempting enhanced fallback parsing")
            val fallbackAnalysis = parseAnalysisContentEnhanced(content)
            Result.success(fallbackAnalysis)
            
        } catch (e: Exception) {
            println("AIService: Analysis parsing failed: ${e.message}")
            android.util.Log.w("MangaLearnJP", "AIService: Analysis parsing failed: ${e.message}")
            Logger.w(Logger.Category.AI_SERVICE, "Analysis parsing failed, creating basic fallback: ${e.message}")
            
            // Basic fallback: create a simple analysis with the raw content
            val fallbackAnalysis = TextAnalysis(
                originalText = "Content received but failed to parse as structured data",
                vocabulary = emptyList(),
                grammarPatterns = emptyList(),
                translation = content.take(500), // Limit translation length
                context = "Analysis parsing failed. This may be due to API response format. Raw content available in translation field."
            )
            
            println("AIService: Created basic fallback analysis with translation: ${fallbackAnalysis.translation.take(100)}...")
            android.util.Log.d("MangaLearnJP", "AIService: Created basic fallback analysis")
            Logger.i(Logger.Category.AI_SERVICE, "Created basic fallback analysis")
            Result.success(fallbackAnalysis)
        }
    }
    
    private fun cleanJsonContent(content: String): String {
        var cleaned = content.trim()
        
        // Remove markdown code blocks if present
        if (cleaned.contains("```json")) {
            cleaned = cleaned.substringAfter("```json")
                .substringBefore("```")
                .trim()
        } else if (cleaned.contains("```")) {
            cleaned = cleaned.substringAfter("```")
                .substringBefore("```")
                .trim()
        }
        
        // Remove any leading/trailing non-JSON characters
        val startIndex = cleaned.indexOfFirst { it == '{' || it == '[' }
        val endIndex = cleaned.indexOfLast { it == '}' || it == ']' }
        
        if (startIndex != -1 && endIndex != -1 && startIndex <= endIndex) {
            cleaned = cleaned.substring(startIndex, endIndex + 1)
        }
        
        return cleaned
    }
    
    private fun isValidJsonStructure(content: String): Boolean {
        val trimmed = content.trim()
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
               (trimmed.startsWith("[") && trimmed.endsWith("]"))
    }
    
    /**
     * Enhanced JSON parsing with validation based on Android Kotlin best practices
     * Implements defensive parsing with null safety and proper error handling
     */
    private fun parseJsonWithValidation(jsonContent: String): TextAnalysis? {
        return try {
            // First attempt: Direct parsing
            val analysis = gson.fromJson(jsonContent, TextAnalysis::class.java)
            
            // Validate parsed object - ensure critical fields are not null/empty
            if (analysis != null && validateTextAnalysis(analysis)) {
                Logger.i(Logger.Category.AI_SERVICE, "JSON parsing successful with validation")
                return analysis
            }
            
            // Second attempt: Try parsing as JsonObject first for better error handling
            val jsonObject = gson.fromJson(jsonContent, JsonObject::class.java)
            if (jsonObject != null) {
                return parseFromJsonObject(jsonObject)
            }
            
            Logger.w(Logger.Category.AI_SERVICE, "JSON parsing validation failed")
            null
        } catch (e: JsonSyntaxException) {
            Logger.w(Logger.Category.AI_SERVICE, "JSON syntax error in enhanced parser: ${e.message}")
            null
        } catch (e: Exception) {
            Logger.w(Logger.Category.AI_SERVICE, "Unexpected error in JSON parsing: ${e.message}")
            null
        }
    }
    
    /**
     * Validates TextAnalysis object to ensure it has meaningful content
     */
    private fun validateTextAnalysis(analysis: TextAnalysis): Boolean {
        return !analysis.translation.isNullOrBlank() || 
               !analysis.originalText.isNullOrBlank() ||
               analysis.vocabulary.isNotEmpty() ||
               analysis.grammarPatterns.isNotEmpty()
    }
    
    /**
     * Parse TextAnalysis from JsonObject with defensive null checking
     */
    private fun parseFromJsonObject(jsonObject: JsonObject): TextAnalysis? {
        return try {
            val originalText = jsonObject.get("originalText")?.asString ?: 
                              jsonObject.get("original_text")?.asString ?: 
                              jsonObject.get("text")?.asString ?: ""
            
            val translation = jsonObject.get("translation")?.asString ?: 
                             jsonObject.get("english")?.asString ?: 
                             jsonObject.get("meaning")?.asString ?: ""
            
            val context = jsonObject.get("context")?.asString ?: 
                         jsonObject.get("situation")?.asString ?: ""
            
            // Parse vocabulary array safely
            val vocabulary = try {
                val vocabArray = jsonObject.getAsJsonArray("vocabulary")
                vocabArray?.map { element ->
                    val vocabObj = element.asJsonObject
                    VocabularyItem(
                        word = vocabObj.get("word")?.asString ?: "",
                        reading = vocabObj.get("reading")?.asString ?: "",
                        meaning = vocabObj.get("meaning")?.asString ?: "",
                        partOfSpeech = vocabObj.get("partOfSpeech")?.asString ?: ""
                    )
                } ?: emptyList()
            } catch (e: Exception) {
                Logger.w(Logger.Category.AI_SERVICE, "Failed to parse vocabulary: ${e.message}")
                emptyList<VocabularyItem>()
            }
            
            // Parse grammar patterns array safely
            val grammarPatterns = try {
                val grammarArray = jsonObject.getAsJsonArray("grammarPatterns")
                grammarArray?.map { element ->
                    val grammarObj = element.asJsonObject
                    GrammarPattern(
                        pattern = grammarObj.get("pattern")?.asString ?: "",
                        explanation = grammarObj.get("explanation")?.asString ?: "",
                        example = grammarObj.get("example")?.asString ?: "",
                        difficulty = grammarObj.get("difficulty")?.asString ?: ""
                    )
                } ?: emptyList()
            } catch (e: Exception) {
                Logger.w(Logger.Category.AI_SERVICE, "Failed to parse grammar patterns: ${e.message}")
                emptyList<GrammarPattern>()
            }
            
            TextAnalysis(
                originalText = originalText,
                translation = translation,
                vocabulary = vocabulary,
                grammarPatterns = grammarPatterns,
                context = context
            )
        } catch (e: Exception) {
            Logger.w(Logger.Category.AI_SERVICE, "Failed to parse from JsonObject: ${e.message}")
            null
        }
    }
    
    private fun parseAnalysisContentEnhanced(content: String): TextAnalysis {
        return try {
            // Use the improved JSON cleaning function
            val cleanedContent = cleanJsonContent(content)
            
            // Try to parse as JSON if it looks like valid JSON structure
            if (isValidJsonStructure(cleanedContent)) {
                try {
                    val analysis = gson.fromJson(cleanedContent, TextAnalysis::class.java)
                    if (analysis != null && !analysis.translation.isNullOrBlank()) {
                        Logger.i(Logger.Category.AI_SERVICE, "Enhanced JSON parsing successful")
                        return analysis
                    }
                } catch (e: JsonSyntaxException) {
                    Logger.w(Logger.Category.AI_SERVICE, "Enhanced JSON parsing failed: ${e.message}")
                }
            }
            
            Logger.w(Logger.Category.AI_SERVICE, "JSON structure invalid, using intelligent text parsing")
            // Enhanced fallback parsing with better text extraction
            parseContentIntelligently(content)
        } catch (e: Exception) {
            Logger.logError("parseAnalysisContentEnhanced", e)
            createFallbackAnalysis("Enhanced parsing failed: ${e.message}")
        }
    }
    
    private fun parseContentIntelligently(content: String): TextAnalysis {
        val lines = content.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        
        // Enhanced text extraction patterns
        var originalText = extractField(lines, listOf("original", "japanese", "text", "原文"))
        var translation = extractField(lines, listOf("translation", "english", "meaning", "翻訳"))
        val context = extractField(lines, listOf("context", "situation", "scene", "文脈"))
        
        // If structured extraction failed, try to extract Japanese text directly
        if (originalText == "Not found") {
            // Look for Japanese characters in the content
            val japaneseTextPattern = Regex("[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FAF]+")
            val foundJapanese = japaneseTextPattern.findAll(content)
                .map { it.value }
                .distinct()
                .joinToString(" ")
            if (foundJapanese.isNotBlank()) {
                originalText = foundJapanese
            }
        }
        
        // If translation not found, use the content as fallback
        if (translation == "Not found" && content.length < 500) {
            translation = content.take(200) // Limit length for fallback
        }
        
        // Extract vocabulary if present
        val vocabulary = extractVocabularyFromText(content)
        val grammarPatterns = extractGrammarFromText(content)
        
        return TextAnalysis(
            originalText = originalText,
            translation = translation,
            vocabulary = vocabulary,
            grammarPatterns = grammarPatterns,
            context = context
        )
    }
    
    private fun extractField(lines: List<String>, keywords: List<String>): String {
        val foundTexts = mutableListOf<String>()
        
        for (keyword in keywords) {
            // Find all lines that match the keyword, not just the first one
            val matchingLines = lines.filter { it.contains(keyword, ignoreCase = true) && it.contains(":") }
            for (line in matchingLines) {
                val extractedText = line.substringAfter(":")
                    .trim()
                    .removeSurrounding("\"", "\"")
                    .removeSurrounding("'", "'")
                if (extractedText.isNotBlank() && extractedText != "Not found") {
                    foundTexts.add(extractedText)
                }
            }
        }
        
        // If we found multiple text elements, combine them
        return if (foundTexts.isNotEmpty()) {
            foundTexts.joinToString(" | ") // Use separator to distinguish different text elements
        } else {
            "Not found"
        }
    }
    
    private fun extractVocabularyFromText(content: String): List<VocabularyItem> {
        // Simple vocabulary extraction - can be enhanced
        val vocabSection = content.substringAfter("vocabulary", "")
            .substringAfter("words", "")
            .take(500) // Limit to avoid processing too much
        
        return vocabSection.split("\n")
            .filter { it.contains(":") || it.contains("-") }
            .take(10) // Limit vocabulary items
            .map { line ->
                VocabularyItem(
                    word = line.substringBefore(":").substringBefore("-").trim(),
                    reading = "Unknown",
                    meaning = line.substringAfter(":").substringAfter("-").trim(),
                    partOfSpeech = "Unknown",
                    jlptLevel = "Unknown",
                    difficulty = 3
                )
            }
    }
    
    private fun extractGrammarFromText(content: String): List<GrammarPattern> {
        // Simple grammar pattern extraction
        val grammarSection = content.substringAfter("grammar", "")
            .substringAfter("pattern", "")
            .take(300)
        
        return grammarSection.split("\n")
            .filter { it.isNotBlank() && it.length > 5 }
            .take(5)
            .map { pattern ->
                GrammarPattern(
                    pattern = pattern.trim(),
                    explanation = "Grammar pattern found in text",
                    example = pattern.trim(),
                    difficulty = "intermediate"
                )
            }
    }
    
    private fun createFallbackAnalysis(errorMessage: String): TextAnalysis {
        return TextAnalysis(
            originalText = "Analysis failed",
            vocabulary = emptyList(),
            grammarPatterns = emptyList(),
            translation = "Unable to analyze image",
            context = errorMessage
        )
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        Logger.logFunctionEntry("AIService", "bitmapToBase64", mapOf(
            "originalSize" to "${bitmap.width}x${bitmap.height}",
            "byteCount" to bitmap.byteCount.toString()
        ))
        
        val outputStream = ByteArrayOutputStream()
        
        // Compress the bitmap to reduce size
        val quality = 85 // Adjust quality as needed (0-100)
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        
        val byteArray = outputStream.toByteArray()
        Logger.i(Logger.Category.IMAGE, "Compressed image size: ${byteArray.size} bytes (quality: $quality%)")
        
        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        Logger.i(Logger.Category.IMAGE, "Base64 string length: ${base64String.length}")
        
        return base64String
    }
    
    companion object {
        private const val MANGA_ANALYSIS_PROMPT = "Analyze this manga image and extract ALL Japanese text from EVERY text element. IMPORTANT: Find ALL speech bubbles, sound effects, background text, and any other Japanese text visible. Combine ALL text found into a comprehensive analysis. For ALL text elements found, provide: 1. ALL original Japanese text (combined) 2. Complete vocabulary breakdown from ALL text 3. Grammar patterns from ALL text 4. Complete English translation of ALL text 5. Context and cultural notes. Return the response in JSON format with the following structure: { \"originalText\": \"ALL Japanese text found (combined)\", \"vocabulary\": [{ \"word\": \"word\", \"reading\": \"reading\", \"meaning\": \"meaning\", \"partOfSpeech\": \"noun/verb/etc\", \"jlptLevel\": \"N1-N5\", \"difficulty\": 1-5 }], \"grammarPatterns\": [{ \"pattern\": \"grammar pattern\", \"explanation\": \"explanation\", \"example\": \"example\", \"difficulty\": \"beginner/intermediate/advanced\" }], \"translation\": \"Complete English translation of ALL text\", \"context\": \"cultural context and notes for ALL text elements\" }"
    }
}