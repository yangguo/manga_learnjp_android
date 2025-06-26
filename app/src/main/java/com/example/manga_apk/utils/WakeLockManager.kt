package com.example.manga_apk.utils

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log

/**
 * Manages wake locks to prevent the device from going to sleep during long-running operations
 * like AI image analysis, ensuring network connectivity is maintained.
 */
class WakeLockManager(private val context: Context) {
    
    private var wakeLock: PowerManager.WakeLock? = null
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    companion object {
        private const val TAG = "WakeLockManager"
        private const val WAKE_LOCK_TAG = "MangaApp:AIAnalysis"
    }
    
    /**
     * Acquires a partial wake lock to keep the CPU running during AI analysis.
     * This prevents the device from going into deep sleep which can cause network failures
     * for both WiFi and mobile data connections.
     */
    fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld != true) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    WAKE_LOCK_TAG
                ).apply {
                    // Set timeout to 15 minutes as a safety measure for longer AI operations
                    acquire(15 * 60 * 1000L)
                }
                Log.d(TAG, "Wake lock acquired for AI analysis (works for WiFi and mobile data)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock: ${e.message}", e)
        }
    }
    
    /**
     * Releases the wake lock when the operation is complete.
     */
    fun releaseWakeLock() {
        try {
            wakeLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                    Log.d(TAG, "Wake lock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock: ${e.message}", e)
        }
    }
    
    /**
     * Checks if the wake lock is currently held.
     * @return true if the lock is held, false otherwise
     */
    fun isWakeLockHeld(): Boolean {
        return wakeLock?.isHeld == true
    }
    
    /**
     * Executes a block of code while holding a wake lock
     */
    suspend fun <T> withWakeLock(block: suspend () -> T): T {
        println("WakeLockManager: withWakeLock called, acquiring wake lock")
        android.util.Log.d("MangaLearnJP", "WakeLockManager: withWakeLock called, acquiring wake lock")
        acquireWakeLock()
        return try {
            println("WakeLockManager: Wake lock acquired, executing block")
            android.util.Log.d("MangaLearnJP", "WakeLockManager: Wake lock acquired, executing block")
            block()
        } finally {
            println("WakeLockManager: Block completed, releasing wake lock")
            android.util.Log.d("MangaLearnJP", "WakeLockManager: Block completed, releasing wake lock")
            releaseWakeLock()
        }
    }
}