package com.example.manga_apk.service

import android.graphics.Bitmap
import android.util.Base64
import com.example.manga_apk.data.*
import com.example.manga_apk.utils.Logger
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class AIService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    // Enhanced prompts for better manga analysis
    private val mangaAnalysisPrompt = """
        You are an expert Japanese language tutor specializing in manga analysis. 
        Analyze the provided manga panel image and extract Japanese text with detailed linguistic analysis.
        
        For each text element found, provide:
        1. Original Japanese text (exact transcription)
        2. Hiragana reading (furigana)
        3. English translation
        4. Vocabulary breakdown with readings and meanings
        5. Grammar patterns and structures
        6. Cultural context and nuances
        7. Difficulty level (N5-N1)
        8. Learning notes for Japanese learners
        
        Focus on:
        - Accurate OCR of Japanese characters (hiragana, katakana, kanji)
        - Speech bubbles and text boxes
        - Sound effects (onomatopoeia)
        - Background text and signs
        - Character emotions and context
        
        Return the analysis in JSON format matching the TextAnalysis structure.
    """.trimIndent()
    
    private val vocabularyFocusPrompt = """
        Focus specifically on vocabulary extraction and learning from this manga panel.
        Identify all Japanese words and provide:
        - Kanji with furigana
        - Word type (noun, verb, adjective, etc.)
        - JLPT level
        - Common usage examples
        - Related words and compounds
        - Memory aids and mnemonics
    """.trimIndent()
    
    init {
        Logger.i(Logger.Category.AI_SERVICE, "Initialized with client: ${client.javaClass.simpleName}")
        Logger.i(Logger.Category.AI_SERVICE, "Client timeouts - Connect: ${client.connectTimeoutMillis}ms, Read: ${client.readTimeoutMillis}ms, Write: ${client.writeTimeoutMillis}ms")
        
        // Add debug logging to check if Logger is working
        println("AIService: Logger initialization test")
        android.util.Log.d("MangaLearnJP", "AIService: Direct Android Log test")
    }
    
    suspend fun testNetworkConnection(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Logger.logFunctionEntry("AIService", "testNetworkConnection")
            
            val request = Request.Builder()
                .url("https://www.google.com")
                .head() // Use HEAD request for faster response
                .build()
            
            val response = client.newCall(request).execute()
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
                AnalysisType.QUICK_TRANSLATION -> "Provide quick Japanese text extraction and translation from this manga panel."
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
            
            val response = client.newCall(request).execute()
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
            val response = client.newCall(request).execute()
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
        val base64Image = bitmapToBase64(bitmap)
        
        // Use the model from custom config
        val modelToUse = config.model
        println("AIService: Using custom API model: $modelToUse")
        println("AIService: Custom API endpoint: '${config.endpoint}'")
        println("AIService: Custom API key length: ${config.apiKey.length}")
        android.util.Log.d("MangaLearnJP", "AIService: Custom API - Endpoint: '${config.endpoint}', Model: '$modelToUse', Key length: ${config.apiKey.length}")
        
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
        
        val requestBuilder = Request.Builder()
            .url(config.endpoint)
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
        
        if (config.apiKey.isNotEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer ${config.apiKey}")
        }
        
        return try {
            println("AIService: Making request to: ${requestBuilder.build().url}")
            android.util.Log.d("MangaLearnJP", "AIService: Making request to: ${requestBuilder.build().url}")
            
            val response = client.newCall(requestBuilder.build()).execute()
            
            println("AIService: Response code: ${response.code}")
            println("AIService: Response message: ${response.message}")
            android.util.Log.d("MangaLearnJP", "AIService: Response code: ${response.code}, message: ${response.message}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    println("AIService: Successful response received, length: ${responseBody.length}")
                    parseOpenAIResponse(responseBody) // Use OpenAI format parser
                } else {
                    val errorMsg = "Empty response body"
                    println("AIService: ERROR - $errorMsg")
                    android.util.Log.e("MangaLearnJP", "AIService: $errorMsg")
                    Result.failure(IOException(errorMsg))
                }
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                val errorMsg = "Custom API call failed: ${response.code} - $errorBody"
                println("AIService: ERROR - $errorMsg")
                println("AIService: Request URL was: ${requestBuilder.build().url}")
                println("AIService: Request headers: ${requestBuilder.build().headers}")
                android.util.Log.e("MangaLearnJP", "AIService: $errorMsg")
                android.util.Log.e("MangaLearnJP", "AIService: Request URL was: ${requestBuilder.build().url}")
                Result.failure(IOException(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = "Exception in custom API call: ${e.message}"
            println("AIService: ERROR - $errorMsg")
            android.util.Log.e("MangaLearnJP", "AIService: $errorMsg", e)
            Result.failure(IOException("Network error: ${e.message}", e))
        }
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
            
            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            
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
            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            
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
            
            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            
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
            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            
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
            
            // Try to parse as JSON first
            val gson = Gson()
            val analysis = gson.fromJson(content, TextAnalysis::class.java)
            
            Logger.i(Logger.Category.AI_SERVICE, "Successfully parsed JSON analysis")
            Result.success(analysis)
            
        } catch (e: Exception) {
            Logger.w(Logger.Category.AI_SERVICE, "JSON parsing failed, attempting fallback parsing: ${e.message}")
            
            // Fallback: create a simple analysis with the raw content
            val fallbackAnalysis = TextAnalysis(
                originalText = "Failed to parse original text",
                vocabulary = emptyList(),
                grammarPatterns = emptyList(),
                translation = content.take(500), // Limit translation length
                context = "Analysis parsing failed. Raw content: ${content.take(200)}..."
            )
            
            Logger.i(Logger.Category.AI_SERVICE, "Created fallback analysis")
            Result.success(fallbackAnalysis)
        }
    }
    
    private fun parseAnalysisContentEnhanced(content: String): TextAnalysis {
        return try {
            // First, try to extract JSON from the content if it's wrapped in markdown
            val jsonContent = if (content.contains("```json")) {
                content.substringAfter("```json")
                    .substringBefore("```")
                    .trim()
            } else if (content.contains("```")) {
                content.substringAfter("```")
                    .substringBefore("```")
                    .trim()
            } else {
                content.trim()
            }
            
            // Try to parse as JSON
            gson.fromJson(jsonContent, TextAnalysis::class.java)
        } catch (e: JsonSyntaxException) {
            Logger.w(Logger.Category.AI_SERVICE, "Enhanced JSON parsing failed, using intelligent fallback")
            
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
        val originalText = extractField(lines, listOf("original", "japanese", "text", "原文"))
        val translation = extractField(lines, listOf("translation", "english", "meaning", "翻訳"))
        val context = extractField(lines, listOf("context", "situation", "scene", "文脈"))
        
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
        for (keyword in keywords) {
            val line = lines.find { it.contains(keyword, ignoreCase = true) && it.contains(":") }
            if (line != null) {
                return line.substringAfter(":")
                    .trim()
                    .removeSurrounding("\"", "\"")
                    .removeSurrounding("'", "'")
            }
        }
        return "Not found"
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
        private const val MANGA_ANALYSIS_PROMPT = "Analyze this manga image and extract all Japanese text. For each text element found, provide: 1. The original Japanese text 2. Vocabulary breakdown with: - Individual words - Hiragana/katakana readings - English meanings - Part of speech - JLPT level if applicable 3. Grammar patterns used 4. English translation 5. Context and cultural notes. Return the response in JSON format with the following structure: { \"originalText\": \"extracted Japanese text\", \"vocabulary\": [{ \"word\": \"word\", \"reading\": \"reading\", \"meaning\": \"meaning\", \"partOfSpeech\": \"noun/verb/etc\", \"jlptLevel\": \"N1-N5\", \"difficulty\": 1-5 }], \"grammarPatterns\": [{ \"pattern\": \"grammar pattern\", \"explanation\": \"explanation\", \"example\": \"example\", \"difficulty\": \"beginner/intermediate/advanced\" }], \"translation\": \"English translation\", \"context\": \"cultural context and notes\" }"
    }
}