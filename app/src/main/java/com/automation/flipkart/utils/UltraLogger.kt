package com.automation.flipkart.utils
 
import android.util.Log
import com.automation.flipkart.BuildConfig
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
 
object UltraLogger {
    private const val TAG = "UltraFlipkartAutomation"
    private val logBuffer = ConcurrentLinkedQueue<String>()
    private val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val performanceCounter = AtomicLong(0)
    private val errorCounter = AtomicLong(0)
    
    fun log(message: String) {
        if (!BuildConfig.ENABLE_LOGGING) return
        
        val timestamp = formatter.format(Date())
        val logEntry = "[$timestamp] $message"
        
        Log.d(TAG, logEntry)
        logBuffer.offer(logEntry)
        
        // Keep only last 500 logs for memory efficiency
        while (logBuffer.size > 500) {
            logBuffer.poll()
        }
    }
    
    fun logPerformance(message: String) {
        performanceCounter.incrementAndGet()
        log("PERF: $message")
    }
    
    fun logError(message: String, exception: Exception? = null) {
        errorCounter.incrementAndGet()
        Log.e(TAG, message, exception)
        log("ERROR: $message${exception?.let { " - ${it.message}" } ?: ""}")
    }
    
    fun getStats(): String {
        return "Perf logs: ${performanceCounter.get()}, Errors: ${errorCounter.get()}, Buffer: ${logBuffer.size}"
    }
    
    fun getLogs(): List<String> = logBuffer.toList()
}