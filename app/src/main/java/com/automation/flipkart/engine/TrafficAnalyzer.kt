package com.automation.flipkart.engine
 
import com.automation.flipkart.config.UltraConfig
import com.automation.flipkart.utils.UltraLogger
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min
 
class TrafficAnalyzer {
    
    private val successCount = AtomicInteger(0)
    private val failureCount = AtomicInteger(0)
    private val avgResponseTime = AtomicLong(0)
    private val lastStrategyUpdate = AtomicLong(0)
    
    private var currentStrategy = Strategy.NORMAL
    
    fun startMonitoring() {
        UltraLogger.log("🔍 Traffic analyzer started")
    }
    
    fun getCurrentStrategy(): Strategy {
        val currentTime = System.currentTimeMillis()
        
        // Update strategy every 5 seconds
        if (currentTime - lastStrategyUpdate.get() > 5000) {
            updateStrategy()
            lastStrategyUpdate.set(currentTime)
        }
        
        return currentStrategy
    }
    
    private fun updateStrategy() {
        val failures = failureCount.get()
        val successes = successCount.get()
        val totalAttempts = failures + successes
        val failureRate = if (totalAttempts > 0) failures.toFloat() / totalAttempts else 0f
        val avgTime = avgResponseTime.get()
        
        currentStrategy = when {
            // Emergency mode: High failures + slow response
            failureRate > 0.7f && avgTime > 3000L -> {
                UltraLogger.log("🚨 EMERGENCY MODE: ${(failureRate * 100).toInt()}% failures, ${avgTime}ms avg")
                Strategy.EMERGENCY
            }
            
            // Aggressive mode: Medium failures OR slow response
            failureRate > 0.4f || avgTime > 2000L -> {
                UltraLogger.log("⚡ AGGRESSIVE MODE: ${(failureRate * 100).toInt()}% failures, ${avgTime}ms avg")
                Strategy.AGGRESSIVE
            }
            
            // Turbo mode: Low failures + fast response
            failureRate < 0.1f && avgTime < 500L -> {
                UltraLogger.log("🚀 TURBO MODE: ${(failureRate * 100).toInt()}% failures, ${avgTime}ms avg")
                Strategy.TURBO
            }
            
            // Normal mode: Balanced performance
            else -> {
                UltraLogger.log("📊 NORMAL MODE: ${(failureRate * 100).toInt()}% failures, ${avgTime}ms avg")
                Strategy.NORMAL
            }
        }
    }
    
    fun recordPerformance(processingTime: Long, successCount: Int) {
        this.successCount.addAndGet(successCount)
        
        // Update rolling average
        val currentAvg = avgResponseTime.get()
        val newAvg = if (currentAvg == 0L) {
            processingTime
        } else {
            (currentAvg * 3 + processingTime) / 4 // Weighted average
        }
        avgResponseTime.set(newAvg)
    }
    
    fun recordFailure() {
        failureCount.incrementAndGet()
    }
    
    fun stop() {
        UltraLogger.log("🛑 Traffic analyzer stopped")
    }
    
    enum class Strategy(
        val maxProcessingTime: Long,
        val searchTimeout: Long,
        val clickRetries: Int,
        val gestureSpeed: Int
    ) {
        TURBO(100L, 25L, 1, 10),
        NORMAL(200L, 50L, 2, 25),
        AGGRESSIVE(500L, 100L, 5, 50),
        EMERGENCY(2000L, 200L, 10, 100)
    }
}