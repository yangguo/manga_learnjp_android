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

class AIService {
    private val client = OkHttpClient()
    private val gson = Gson()
    
    suspend fun analyzeImage(
        bitmap: Bitmap,
        config: AIConfig
    ): Result<TextAnalysis> = withContext(Dispatchers.IO) {
        try {
            // Validate configuration first
            if (config.apiKey.isEmpty()) {
                return@withContext Result.failure(
                    IllegalArgumentException("API key is required")
                )
            }
            
            when (config.provider) {
                AIProvider.OPENAI -> analyzeWithOpenAI(bitmap, config)
                AIProvider.GEMINI -> analyzeWithGemini(bitmap, config)
                AIProvider.CUSTOM -> analyzeWithCustomAPI(bitmap, config)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun analyzeWithOpenAI(
        bitmap: Bitmap,
        config: AIConfig
    ): Result<TextAnalysis> {
        val base64Image = bitmapToBase64(bitmap)
        
        val requestBody = JsonObject().apply {
            addProperty("model", config.visionModel)
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
        
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                parseOpenAIResponse(responseBody)
            } else {
                val errorBody = response.body?.string()
                Result.failure(IOException("OpenAI API call failed: ${response.code} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(IOException("Network error: ${e.message}", e))
        }
    }
    
    private suspend fun analyzeWithGemini(
        bitmap: Bitmap,
        config: AIConfig
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
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=${config.apiKey}")
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
        config: AIConfig
    ): Result<TextAnalysis> {
        val base64Image = bitmapToBase64(bitmap)
        
        val requestBody = JsonObject().apply {
            addProperty("model", config.customModel)
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
            .url(config.customEndpoint)
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
            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            val content = jsonResponse
                .getAsJsonArray("choices")
                .get(0).asJsonObject
                .getAsJsonObject("message")
                .get("content").asString
            
            parseAnalysisContent(content)
        } catch (e: Exception) {
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
            // Parse the structured response from AI
            val analysis = gson.fromJson(content, TextAnalysis::class.java)
            Result.success(analysis)
        } catch (e: Exception) {
            // Fallback: create a basic analysis if JSON parsing fails
            val fallbackAnalysis = TextAnalysis(
                originalText = content,
                vocabulary = emptyList(),
                grammarPatterns = emptyList(),
                translation = "Analysis parsing failed. Raw content: $content",
                context = "Error in parsing AI response"
            )
            Result.success(fallbackAnalysis)
        }
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
    
    companion object {
        private const val MANGA_ANALYSIS_PROMPT = "Analyze this manga image and extract all Japanese text. For each text element found, provide: 1. The original Japanese text 2. Vocabulary breakdown with: - Individual words - Hiragana/katakana readings - English meanings - Part of speech - JLPT level if applicable 3. Grammar patterns used 4. English translation 5. Context and cultural notes. Return the response in JSON format with the following structure: { \"originalText\": \"extracted Japanese text\", \"vocabulary\": [{ \"word\": \"word\", \"reading\": \"reading\", \"meaning\": \"meaning\", \"partOfSpeech\": \"noun/verb/etc\", \"jlptLevel\": \"N1-N5\", \"difficulty\": 1-5 }], \"grammarPatterns\": [{ \"pattern\": \"grammar pattern\", \"explanation\": \"explanation\", \"example\": \"example\", \"difficulty\": \"beginner/intermediate/advanced\" }], \"translation\": \"English translation\", \"context\": \"cultural context and notes\" }"
    }
}