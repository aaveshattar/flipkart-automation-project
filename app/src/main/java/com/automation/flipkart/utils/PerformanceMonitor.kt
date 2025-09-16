package com.automation.flipkart.utils

 

import java.util.concurrent.atomic.AtomicLong

import java.util.concurrent.atomic.AtomicInteger

import java.util.concurrent.ConcurrentHashMap

import java.util.concurrent.locks.ReentrantReadWriteLock

import kotlin.concurrent.read

import kotlin.concurrent.write

 

class PerformanceMonitor {

    

    private val totalProcessingTime = AtomicLong(0)

    private val operationCount = AtomicInteger(0)

    private val maxProcessingTime = AtomicLong(0)

    private val minProcessingTime = AtomicLong(Long.MAX_VALUE)

    private val successCount = AtomicInteger(0)

    private val failureCount = AtomicInteger(0)

    

    // Thread-safe collections for detailed metrics

    private val operationTypes = ConcurrentHashMap<String, OperationStats>()

    private val recentOperations = ConcurrentHashMap<Long, OperationData>()

    private val lock = ReentrantReadWriteLock()

    

    // Performance thresholds

    companion object {

        private const val SLOW_OPERATION_THRESHOLD = 1000L // 1 second

        private const val VERY_SLOW_OPERATION_THRESHOLD = 5000L // 5 seconds

        private const val MAX_RECENT_OPERATIONS = 100

    }

    

    data class OperationStats(

        var count: Int = 0,

        var totalTime: Long = 0L,

        var maxTime: Long = 0L,

        var minTime: Long = Long.MAX_VALUE,

        var successRate: Float = 0f

    )

    

    data class OperationData(

        val timestamp: Long,

        val duration: Long,

        val operationType: String,

        val success: Boolean

    )

    

    fun recordProcessingTime(time: Long, operationType: String = "general", success: Boolean = true) {

        if (time < 0) {

            UltraLogger.logError("Invalid processing time: $time", null)

            return

        }

        

        try {

            // Update general counters

            totalProcessingTime.addAndGet(time)

            val currentCount = operationCount.incrementAndGet()

            

            if (success) {

                successCount.incrementAndGet()

            } else {

                failureCount.incrementAndGet()

            }

            

            // Update min/max atomically

            updateMinTime(time)

            updateMaxTime(time)

            

            // Update operation-specific stats

            updateOperationStats(operationType, time, success)

            

            // Store recent operation for trend analysis

            storeRecentOperation(time, operationType, success)

            

            // Log performance warnings

            logPerformanceWarnings(time, operationType)

            

            // Clean up old data periodically

            if (currentCount % 50 == 0) {

                cleanupOldData()

            }

            

        } catch (e: Exception) {

            UltraLogger.logError("Error recording performance time", e)

        }

    }

    

    private fun updateMinTime(time: Long) {

        var currentMin = minProcessingTime.get()

        while (time < currentMin && !minProcessingTime.compareAndSet(currentMin, time)) {

            currentMin = minProcessingTime.get()

        }

    }

    

    private fun updateMaxTime(time: Long) {

        var currentMax = maxProcessingTime.get()

        while (time > currentMax && !maxProcessingTime.compareAndSet(currentMax, time)) {

            currentMax = maxProcessingTime.get()

        }

    }

    

    private fun updateOperationStats(operationType: String, time: Long, success: Boolean) {

        lock.write {

            val stats = operationTypes.getOrPut(operationType) { OperationStats() }

            

            stats.count++

            stats.totalTime += time

            stats.maxTime = maxOf(stats.maxTime, time)

            stats.minTime = minOf(stats.minTime, time)

            

            // Calculate success rate

            val totalOps = stats.count

            val successOps = if (success) {

                (stats.successRate * (totalOps - 1) + 1).toInt()

            } else {

                (stats.successRate * (totalOps - 1)).toInt()

            }

            

            stats.successRate = successOps.toFloat() / totalOps

        }

    }

    

    private fun storeRecentOperation(time: Long, operationType: String, success: Boolean) {

        val timestamp = System.currentTimeMillis()

        val operation = OperationData(timestamp, time, operationType, success)

        

        lock.write {

            recentOperations[timestamp] = operation

            

            // Remove oldest entries if we exceed the limit

            if (recentOperations.size > MAX_RECENT_OPERATIONS) {

                val oldestKey = recentOperations.keys.minOrNull()

                oldestKey?.let { recentOperations.remove(it) }

            }

        }

    }

    

    private fun logPerformanceWarnings(time: Long, operationType: String) {

        when {

            time > VERY_SLOW_OPERATION_THRESHOLD -> {

                UltraLogger.logError("VERY SLOW operation: $operationType took ${time}ms", null)

            }

            time > SLOW_OPERATION_THRESHOLD -> {

                UltraLogger.log("SLOW operation: $operationType took ${time}ms")

            }

        }

    }

    

    private fun cleanupOldData() {

        try {

            val cutoffTime = System.currentTimeMillis() - (5 * 60 * 1000L) // 5 minutes

            

            lock.write {

                val keysToRemove = recentOperations.entries

                    .filter { it.value.timestamp < cutoffTime }

                    .map { it.key }

                

                keysToRemove.forEach { recentOperations.remove(it) }

            }

            

        } catch (e: Exception) {

            UltraLogger.logError("Error during performance data cleanup", e)

        }

    }

    

    fun getAverageProcessingTime(): Long {

        val count = operationCount.get()

        return if (count > 0) {

            totalProcessingTime.get() / count

        } else {

            0L

        }

    }

    

    fun getSuccessRate(): Float {

        val total = operationCount.get()

        return if (total > 0) {

            successCount.get().toFloat() / total

        } else {

            0f

        }

    }

    

    fun getOperationStats(operationType: String): OperationStats? {

        return lock.read {

            operationTypes[operationType]?.copy()

        }

    }

    

    fun getAllOperationStats(): Map<String, OperationStats> {

        return lock.read {

            operationTypes.toMap()

        }

    }

    

    fun getRecentPerformanceTrend(): List<Long> {

        return lock.read {

            recentOperations.values

                .sortedBy { it.timestamp }

                .takeLast(20)

                .map { it.duration }

        }

    }

    

    fun isPerformanceDegrading(): Boolean {

        val trend = getRecentPerformanceTrend()

        if (trend.size < 10) return false

        

        val firstHalf = trend.take(trend.size / 2).average()

        val secondHalf = trend.drop(trend.size / 2).average()

        

        return secondHalf > firstHalf * 1.5 // 50% increase indicates degradation

    }

    

    fun logStats() {

        try {

            val count = operationCount.get()

            if (count <= 0) {

                UltraLogger.log("No performance data available")

                return

            }

            

            val avgTime = getAverageProcessingTime()

            val successRate = getSuccessRate()

            val minTime = minProcessingTime.get()

            val maxTime = maxProcessingTime.get()

            

            val statsMessage = buildString {

                append("Performance Stats: ")

                append("Operations: $count, ")

                append("Avg: ${avgTime}ms, ")

                append("Min: ${if (minTime == Long.MAX_VALUE) 0 else minTime}ms, ")

                append("Max: ${maxTime}ms, ")

                append("Success: ${String.format("%.1f", successRate * 100)}%")

            }

            

            UltraLogger.logPerformance(statsMessage)

            

            // Log operation-specific stats

            lock.read {

                operationTypes.forEach { (type, stats) ->

                    val typeAvg = if (stats.count > 0) stats.totalTime / stats.count else 0L

                    UltraLogger.logPerformance(

                        "$type: ${stats.count} ops, avg: ${typeAvg}ms, success: ${String.format("%.1f", stats.successRate * 100)}%"

                    )

                }

            }

            

            // Check for performance degradation

            if (isPerformanceDegrading()) {

                UltraLogger.log("WARNING: Performance degradation detected!")

            }

            

        } catch (e: Exception) {

            UltraLogger.logError("Error logging performance stats", e)

        }

    }

    

    fun reset() {

        try {

            totalProcessingTime.set(0)

            operationCount.set(0)

            maxProcessingTime.set(0)

            minProcessingTime.set(Long.MAX_VALUE)

            successCount.set(0)

            failureCount.set(0)

            

            lock.write {

                operationTypes.clear()

                recentOperations.clear()

            }

            

            UltraLogger.log("Performance monitor reset")

            

        } catch (e: Exception) {

            UltraLogger.logError("Error resetting performance monitor", e)

        }

    }

    

    fun getHealthStatus(): String {

        return try {

            val avgTime = getAverageProcessingTime()

            val successRate = getSuccessRate()

            val isDegrading = isPerformanceDegrading()

            

            when {

                avgTime > VERY_SLOW_OPERATION_THRESHOLD || successRate < 0.5f -> "CRITICAL"

                avgTime > SLOW_OPERATION_THRESHOLD || successRate < 0.8f || isDegrading -> "WARNING"

                else -> "HEALTHY"

            }

        } catch (e: Exception) {

            UltraLogger.logError("Error getting health status", e)

            "ERROR"

        }

    }

    

    fun exportMetrics(): String {

        return try {

            val count = operationCount.get()

            val avgTime = getAverageProcessingTime()

            val successRate = getSuccessRate()

            val healthStatus = getHealthStatus()

            val trend = getRecentPerformanceTrend()

            

            buildString {

                appendLine("=== PERFORMANCE METRICS EXPORT ===")

                appendLine("Total Operations: $count")

                appendLine("Average Time: ${avgTime}ms")

                appendLine("Success Rate: ${String.format("%.2f", successRate * 100)}%")

                appendLine("Health Status: $healthStatus")

                appendLine("Min Time: ${if (minProcessingTime.get() == Long.MAX_VALUE) 0 else minProcessingTime.get()}ms")

                appendLine("Max Time: ${maxProcessingTime.get()}ms")

                appendLine("Recent Trend: ${trend.joinToString(", ")}ms")

                appendLine()

                appendLine("=== OPERATION BREAKDOWN ===")

                

                lock.read {

                    operationTypes.forEach { (type, stats) ->

                        val typeAvg = if (stats.count > 0) stats.totalTime / stats.count else 0L

                        appendLine("$type: ${stats.count} ops, avg: ${typeAvg}ms, success: ${String.format("%.2f", stats.successRate * 100)}%")

                    }

                }

                

                appendLine("=== END METRICS ===")

            }

        } catch (e: Exception) {

            UltraLogger.logError("Error exporting metrics", e)

            "Error exporting metrics: ${e.message}"

        }

    }

}