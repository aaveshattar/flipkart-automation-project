package com.automation.flipkart.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

object Logger {
    private const val TAG = "FlipkartAutomation"
    private val logBuffer = ConcurrentLinkedQueue<String>()
    private val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    
    fun log(message: String) {
        val timestamp = formatter.format(Date())
        val logEntry = "[$timestamp] $message"
        Log.d(TAG, logEntry)
        logBuffer.offer(logEntry)
        
        while (logBuffer.size > 1000) {
            logBuffer.poll()
        }
    }
    
    fun logSuccess(message: String) {
        log("✅ SUCCESS: $message")
    }
    
    fun logWarning(message: String) {
        Log.w(TAG, message)
        log("⚠️ WARNING: $message")
    }
    
    fun logError(message: String, exception: Exception? = null) {
        Log.e(TAG, message, exception)
        log("❌ ERROR: $message${exception?.let { " - ${it.message}" } ?: ""}")
    }
    
    fun logPerformance(message: String) {
        log("📊 PERF: $message")
    }
    
    fun getLogs(): List<String> = logBuffer.toList()
}