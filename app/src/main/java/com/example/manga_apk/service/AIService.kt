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
            Logger.logError("testNetworkConnection", "Network test failed", e)
            false
        }
    }
    
    suspend fun analyzeImage(
        bitmap: Bitmap,
        config: AIConfig
    ): Result<TextAnalysis> = withContext(Dispatchers.IO) {
        try {
            Logger.logFunctionEntry("AIService", "analyzeImage", mapOf(
                "bitmapSize" to "${bitmap.width}x${bitmap.height}",
                "primaryProvider" to config.primaryProvider.toString(),
                "fallbackEnabled" to config.enableFallback.toString()
            ))
            
            val providersToTry = config.getConfiguredProviders()
            Logger.i(Logger.Category.AI_SERVICE, "Configured providers to try: $providersToTry")
            
            if (providersToTry.isEmpty()) {
                Logger.logError("analyzeImage", "No configured providers found")
                return@withContext Result.failure(
                    IllegalArgumentException("No API providers are configured")
                )
            }
            
            var lastException: Exception? = null
            
            for (provider in providersToTry) {
                Logger.i(Logger.Category.AI_SERVICE, "Attempting analysis with provider: $provider")
                
                val result = when (provider) {
                    AIProvider.OPENAI -> {
                        Logger.i(Logger.Category.AI_SERVICE, "Using OpenAI provider")
                        analyzeWithOpenAI(bitmap, config.openaiConfig)
                    }
                    AIProvider.GEMINI -> {
                        Logger.i(Logger.Category.AI_SERVICE, "Using Gemini provider")
                        analyzeWithGemini(bitmap, config.geminiConfig)
                    }
                    AIProvider.CUSTOM -> {
                        Logger.i(Logger.Category.AI_SERVICE, "Using Custom provider")
                        analyzeWithCustomAPI(bitmap, config.customConfig)
                    }
                }
                
                if (result.isSuccess) {
                    Logger.i(Logger.Category.AI_SERVICE, "Analysis successful with provider: $provider")
                    return@withContext result
                } else {
                    lastException = result.exceptionOrNull() as? Exception
                    Logger.logError("analyzeImage", "Analysis failed with provider: $provider", lastException)
                    
                    // If fallback is disabled and this is the primary provider, return the failure
                    if (!config.enableFallback && provider == config.primaryProvider) {
                        Logger.i(Logger.Category.AI_SERVICE, "Fallback disabled, returning failure from primary provider")
                        return@withContext result
                    }
                }
            }
            
            Logger.logError("analyzeImage", "All providers failed", lastException)
            Result.failure(lastException ?: Exception("All configured providers failed"))
        } catch (e: Exception) {
            Logger.logError("analyzeImage", "Unexpected error", e)
            Result.failure(e)
        }
    }
    
    private suspend fun analyzeWithOpenAI(
        bitmap: Bitmap,
        config: OpenAIConfig
    ): Result<TextAnalysis> {
        Logger.logFunctionEntry("AIService", "analyzeWithOpenAI", mapOf(
            "bitmapSize" to "${bitmap.width}x${bitmap.height}",
            "visionModel" to config.visionModel
        ))
        
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
            
            val response = client.newCall(request).execute()
            Logger.i(Logger.Category.NETWORK, "Response received, status: ${response.code}")
            Logger.i(Logger.Category.NETWORK, "Response headers: ${response.headers}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Logger.i(Logger.Category.NETWORK, "Response body length: ${responseBody?.length ?: 0}")
                if (responseBody != null && responseBody.length > 100) {
                    Logger.i(Logger.Category.NETWORK, "Response body preview: ${responseBody.take(200)}...")
                } else {
                    Logger.i(Logger.Category.NETWORK, "Full response body: $responseBody")
                }
                parseOpenAIResponse(responseBody)
            } else {
                val errorBody = response.body?.string()
                Logger.logError("analyzeWithOpenAI", "API call failed with status ${response.code}")
                Logger.logError("analyzeWithOpenAI", "Error body: $errorBody")
                Result.failure(IOException("OpenAI API call failed: ${response.code} - $errorBody"))
            }
        } catch (e: SocketTimeoutException) {
            Logger.logError("analyzeWithOpenAI", "Request timeout", e)
            Result.failure(e)
        } catch (e: ConnectException) {
            Logger.logError("analyzeWithOpenAI", "Connection failed", e)
            Result.failure(e)
        } catch (e: UnknownHostException) {
            Logger.logError("analyzeWithOpenAI", "Host unknown", e)
            Result.failure(e)
        } catch (e: IOException) {
            Logger.logError("analyzeWithOpenAI", "IO error", e)
            Result.failure(e)
        } catch (e: Exception) {
            Logger.logError("analyzeWithOpenAI", "Unexpected error", e)
            Result.failure(e)
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
            Logger.logError("parseOpenAIResponse", "Error parsing response", e)
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
                vocabulary = emptyList(),
                grammar = emptyList(),
                translation = content.take(500), // Limit translation length
                difficulty = "Unknown",
                summary = "Analysis parsing failed. Raw content: ${content.take(200)}..."
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