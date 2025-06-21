package com.example.manga_apk.service

import android.graphics.Bitmap
import android.util.Base64
import com.example.manga_apk.data.*
import com.example.manga_apk.utils.Logger
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
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
                        mapOf("text" to MANGA_ANALYSIS_PROMPT),
                        mapOf(
                            "inline_data" to mapOf(
                                "mime_type" to "image/jpeg",
                                "data" to base64Image
                            )
                        )
                    )
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
                Result.failure(IOException("Gemini API call failed: ${response.code} - $errorBody"))
            }
        } catch (e: Exception) {
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
            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                parseOpenAIResponse(responseBody) // Use OpenAI format parser
            } else {
                val errorBody = response.body?.string()
                Result.failure(IOException("Custom API call failed: ${response.code} - $errorBody"))
            }
        } catch (e: Exception) {
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
    
    private fun parseGeminiResponse(responseBody: String?): Result<TextAnalysis> {
        return try {
            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            val content = jsonResponse
                .getAsJsonArray("candidates")
                .get(0).asJsonObject
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).asJsonObject
                .get("text").asString
            
            parseAnalysisContent(content)
        } catch (e: Exception) {
            Result.failure(e)
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