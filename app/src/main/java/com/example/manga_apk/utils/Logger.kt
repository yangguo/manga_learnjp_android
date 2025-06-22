package com.example.manga_apk.utils

import android.util.Log
import com.example.manga_apk.ui.DebugLogCollector
import java.text.SimpleDateFormat
import java.util.*

/**
 * Centralized logging utility for the Manga Learn JP app
 * Provides structured logging with different levels and categories
 */
object Logger {
    private const val TAG = "MangaLearnJP"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    // Log categories for better organization
    enum class Category(val prefix: String) {
        UI("[UI]"),
        VIEWMODEL("[VM]"),
        AI_SERVICE("[AI]"),
        NETWORK("[NET]"),
        IMAGE("[IMG]"),
        CONFIG("[CFG]"),
        ERROR("[ERR]"),
        DEBUG("[DBG]")
    }
    
    // Log levels
    enum class Level {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }
    
    private fun formatMessage(category: Category, message: String): String {
        val timestamp = dateFormat.format(Date())
        return "$timestamp ${category.prefix} $message"
    }
    
    // Verbose logging
    fun v(category: Category, message: String, throwable: Throwable? = null) {
        val formattedMessage = formatMessage(category, message)
        if (throwable != null) {
            Log.v(TAG, formattedMessage, throwable)
            DebugLogCollector.addLog("VERBOSE", "$formattedMessage\n${throwable.stackTraceToString()}")
        } else {
            Log.v(TAG, formattedMessage)
            DebugLogCollector.addLog("VERBOSE", formattedMessage)
        }
    }
    
    // Debug logging
    fun d(category: Category, message: String, throwable: Throwable? = null) {
        val formattedMessage = formatMessage(category, message)
        if (throwable != null) {
            Log.d(TAG, formattedMessage, throwable)
            DebugLogCollector.addLog("DEBUG", "$formattedMessage\n${throwable.stackTraceToString()}")
        } else {
            Log.d(TAG, formattedMessage)
            DebugLogCollector.addLog("DEBUG", formattedMessage)
        }
    }
    
    // Info logging
    fun i(category: Category, message: String, throwable: Throwable? = null) {
        val formattedMessage = formatMessage(category, message)
        if (throwable != null) {
            Log.i(TAG, formattedMessage, throwable)
            DebugLogCollector.addLog("INFO", "$formattedMessage\n${throwable.stackTraceToString()}")
        } else {
            Log.i(TAG, formattedMessage)
            DebugLogCollector.addLog("INFO", formattedMessage)
        }
    }
    
    // Warning logging
    fun w(category: Category, message: String, throwable: Throwable? = null) {
        val formattedMessage = formatMessage(category, message)
        if (throwable != null) {
            Log.w(TAG, formattedMessage, throwable)
            DebugLogCollector.addLog("WARN", "$formattedMessage\n${throwable.stackTraceToString()}")
        } else {
            Log.w(TAG, formattedMessage)
            DebugLogCollector.addLog("WARN", formattedMessage)
        }
    }
    
    // Error logging
    fun e(category: Category, message: String, throwable: Throwable? = null) {
        val formattedMessage = formatMessage(category, message)
        if (throwable != null) {
            Log.e(TAG, formattedMessage, throwable)
            DebugLogCollector.addLog("ERROR", "$formattedMessage\n${throwable.stackTraceToString()}")
        } else {
            Log.e(TAG, formattedMessage)
            DebugLogCollector.addLog("ERROR", formattedMessage)
        }
    }
    
    // Convenience methods for common scenarios
    fun logUIEvent(event: String) {
        d(Category.UI, "Event: $event")
    }
    
    fun logViewModelAction(action: String) {
        d(Category.VIEWMODEL, "Action: $action")
    }
    
    fun logAIServiceCall(provider: String, action: String) {
        i(Category.AI_SERVICE, "Provider: $provider, Action: $action")
    }
    
    fun logNetworkRequest(url: String, method: String = "GET") {
        i(Category.NETWORK, "$method request to: $url")
    }
    
    fun logNetworkResponse(url: String, statusCode: Int, responseSize: Int = -1) {
        val sizeInfo = if (responseSize >= 0) ", Size: ${responseSize}B" else ""
        i(Category.NETWORK, "Response from: $url, Status: $statusCode$sizeInfo")
    }
    
    fun logImageProcessing(action: String, width: Int = -1, height: Int = -1) {
        val sizeInfo = if (width > 0 && height > 0) " (${width}x${height})" else ""
        d(Category.IMAGE, "$action$sizeInfo")
    }
    
    fun logConfigChange(setting: String, value: String) {
        i(Category.CONFIG, "Setting changed: $setting = $value")
    }
    
    fun logError(context: String, error: Throwable) {
        e(Category.ERROR, "Error in $context: ${error.message}", error)
    }
    
    fun logError(context: String, message: String) {
        e(Category.ERROR, "Error in $context: $message")
    }
    
    // Method to log function entry/exit for debugging
    fun logFunctionEntry(className: String, functionName: String, params: Map<String, Any> = emptyMap()) {
        val paramsStr = if (params.isNotEmpty()) {
            params.entries.joinToString(", ") { "${it.key}=${it.value}" }
        } else {
            "no params"
        }
        d(Category.DEBUG, "→ $className.$functionName($paramsStr)")
    }
    
    fun logFunctionExit(className: String, functionName: String, result: String = "void") {
        d(Category.DEBUG, "← $className.$functionName returns: $result")
    }
    
    // Method to log state changes
    fun logStateChange(component: String, from: String, to: String) {
        i(Category.DEBUG, "State change in $component: $from → $to")
    }
    
    // Method to log performance metrics
    fun logPerformance(operation: String, durationMs: Long) {
        i(Category.DEBUG, "Performance: $operation took ${durationMs}ms")
    }
}