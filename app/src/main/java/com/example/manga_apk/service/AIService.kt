package com.example.manga_apk.service

import android.graphics.Bitmap
import android.util.Base64
import com.example.manga_apk.data.*
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
        println("AIService: Initialized with client: ${client.javaClass.simpleName}")
        println("AIService: Client timeouts - Connect: ${client.connectTimeoutMillis}, Read: ${client.readTimeoutMillis}, Write: ${client.writeTimeoutMillis}")
    }
    
    suspend fun testNetworkConnection(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            println("AIService: Testing network connection to httpbin.org")
            val request = Request.Builder()
                .url("https://httpbin.org/get")
                .build()
            
            val response = client.newCall(request).execute()
            println("AIService: Test request completed with status: ${response.code}")
            
            if (response.isSuccessful) {
                Result.success("Network connection test successful")
            } else {
                Result.failure(Exception("Network test failed with status: ${response.code}"))
            }
        } catch (e: Exception) {
            println("AIService: Network test failed: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun analyzeImage(
        bitmap: Bitmap,
        config: AIConfig
    ): Result<TextAnalysis> = withContext(Dispatchers.IO) {
        try {
            println("AIService: Starting analyzeImage")
            println("AIService: Bitmap size: ${bitmap.width}x${bitmap.height}")
            println("AIService: Primary Provider: ${config.primaryProvider}")
            println("AIService: Fallback enabled: ${config.enableFallback}")
            
            val providersToTry = config.getConfiguredProviders()
            println("AIService: Configured providers to try: $providersToTry")
            
            if (providersToTry.isEmpty()) {
                println("AIService: No configured providers found")
                return@withContext Result.failure(
                    IllegalArgumentException("No API providers are configured")
                )
            }
            
            var lastException: Exception? = null
            
            for (provider in providersToTry) {
                println("AIService: Trying provider: $provider")
                
                val result = when (provider) {
                    AIProvider.OPENAI -> {
                        println("AIService: Using OpenAI provider")
                        analyzeWithOpenAI(bitmap, config.openaiConfig)
                    }
                    AIProvider.GEMINI -> {
                        println("AIService: Using Gemini provider")
                        analyzeWithGemini(bitmap, config.geminiConfig)
                    }
                    AIProvider.CUSTOM -> {
                        println("AIService: Using Custom provider")
                        analyzeWithCustomAPI(bitmap, config.customConfig)
                    }
                }
                
                if (result.isSuccess) {
                    println("AIService: Successfully analyzed with provider: $provider")
                    return@withContext result
                } else {
                    lastException = result.exceptionOrNull() as? Exception
                    println("AIService: Provider $provider failed: ${lastException?.message}")
                    
                    // If fallback is disabled and this is the primary provider, return the failure
                    if (!config.enableFallback && provider == config.primaryProvider) {
                        println("AIService: Fallback disabled, returning failure from primary provider")
                        return@withContext result
                    }
                }
            }
            
            println("AIService: All providers failed")
            Result.failure(lastException ?: Exception("All configured providers failed"))
        } catch (e: Exception) {
            println("AIService: Exception in analyzeImage: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    private suspend fun analyzeWithOpenAI(
        bitmap: Bitmap,
        config: OpenAIConfig
    ): Result<TextAnalysis> {
        println("AIService: Starting OpenAI analysis")
        
        val base64Image = bitmapToBase64(bitmap)
        println("AIService: Image converted to base64, length: ${base64Image.length}")
        
        // Use the visionModel for OpenAI provider
        val modelToUse = config.visionModel
        println("AIService: Using OpenAI model: $modelToUse")
        
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
        
        println("AIService: Request body prepared")
        println("AIService: Using model: $modelToUse")
        
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        println("AIService: Making API request to OpenAI")
        
        return try {
            println("AIService: Making API request to OpenAI")
            println("AIService: Request URL: https://api.openai.com/v1/chat/completions")
            println("AIService: Request headers: Authorization=Bearer [REDACTED], Content-Type=application/json")
            
            val response = client.newCall(request).execute()
            println("AIService: Response received, status: ${response.code}")
            println("AIService: Response headers: ${response.headers}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                println("AIService: Response body length: ${responseBody?.length ?: 0}")
                if (responseBody != null && responseBody.length > 100) {
                    println("AIService: Response body preview: ${responseBody.take(200)}...")
                } else {
                    println("AIService: Full response body: $responseBody")
                }
                parseOpenAIResponse(responseBody)
            } else {
                val errorBody = response.body?.string()
                println("AIService: API call failed with status ${response.code}")
                println("AIService: Error body: $errorBody")
                Result.failure(IOException("OpenAI API call failed: ${response.code} - $errorBody"))
            }
        } catch (e: Exception) {
            println("AIService: Network error: ${e.message}")
            e.printStackTrace()
            
            val errorMessage = when (e) {
                is UnknownHostException -> "Unable to resolve host. Check internet connection."
                is ConnectException -> "Unable to connect to server. Check internet connection."
                is SocketTimeoutException -> "Request timed out. Server may be overloaded."
                else -> "Network error: ${e.message}"
            }
            
            Result.failure(IOException(errorMessage, e))
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
            println("AIService: Parsing OpenAI response")
            println("AIService: Response body preview: ${responseBody?.take(200)}")
            
            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            val content = jsonResponse
                .getAsJsonArray("choices")
                .get(0).asJsonObject
                .getAsJsonObject("message")
                .get("content").asString
            
            println("AIService: Extracted content length: ${content.length}")
            println("AIService: Content preview: ${content.take(200)}")
            
            parseAnalysisContent(content)
        } catch (e: Exception) {
            println("AIService: Error parsing OpenAI response: ${e.message}")
            e.printStackTrace()
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
            println("AIService: Parsing analysis content")
            println("AIService: Attempting JSON parse of content")
            
            // Parse the structured response from AI
            val analysis = gson.fromJson(content, TextAnalysis::class.java)
            println("AIService: JSON parsing successful")
            println("AIService: Analysis - Original text: ${analysis.originalText.take(50)}")
            Result.success(analysis)
        } catch (e: Exception) {
            println("AIService: JSON parsing failed: ${e.message}")
            println("AIService: Creating fallback analysis")
            
            // Fallback: create a basic analysis if JSON parsing fails
            val fallbackAnalysis = TextAnalysis(
                originalText = content,
                vocabulary = emptyList(),
                grammarPatterns = emptyList(),
                translation = "Analysis parsing failed. Raw content: $content",
                context = "Error in parsing AI response"
            )
            println("AIService: Fallback analysis created")
            Result.success(fallbackAnalysis)
        }
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        println("AIService: Converting bitmap to base64")
        println("AIService: Bitmap config: ${bitmap.config}, size: ${bitmap.width}x${bitmap.height}")
        
        val outputStream = ByteArrayOutputStream()
        val compressionResult = bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        println("AIService: Compression successful: $compressionResult")
        
        val byteArray = outputStream.toByteArray()
        println("AIService: Compressed image size: ${byteArray.size} bytes")
        
        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        println("AIService: Base64 encoding complete, length: ${base64String.length}")
        
        return base64String
    }
    
    companion object {
        private const val MANGA_ANALYSIS_PROMPT = "Analyze this manga image and extract all Japanese text. For each text element found, provide: 1. The original Japanese text 2. Vocabulary breakdown with: - Individual words - Hiragana/katakana readings - English meanings - Part of speech - JLPT level if applicable 3. Grammar patterns used 4. English translation 5. Context and cultural notes. Return the response in JSON format with the following structure: { \"originalText\": \"extracted Japanese text\", \"vocabulary\": [{ \"word\": \"word\", \"reading\": \"reading\", \"meaning\": \"meaning\", \"partOfSpeech\": \"noun/verb/etc\", \"jlptLevel\": \"N1-N5\", \"difficulty\": 1-5 }], \"grammarPatterns\": [{ \"pattern\": \"grammar pattern\", \"explanation\": \"explanation\", \"example\": \"example\", \"difficulty\": \"beginner/intermediate/advanced\" }], \"translation\": \"English translation\", \"context\": \"cultural context and notes\" }"
    }
}