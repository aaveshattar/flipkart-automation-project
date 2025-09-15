package com.automation.flipkart.utils

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger

class PerformanceMonitor {
    private val totalProcessingTime = AtomicLong(0)
    private val operationCount = AtomicInteger(0)
    private val maxProcessingTime = AtomicLong(0)
    private val minProcessingTime = AtomicLong(Long.MAX_VALUE)

    fun recordProcessingTime(time: Long) {
        totalProcessingTime.addAndGet(time)
        operationCount.incrementAndGet()
        
        var currentMax = maxProcessingTime.get()
        while (time > currentMax && !maxProcessingTime.compareAndSet(currentMax, time)) {
            currentMax = maxProcessingTime.get()
        }
        
        var currentMin = minProcessingTime.get()
        while (time < currentMin && !minProcessingTime.compareAndSet(currentMin, time)) {
            currentMin = minProcessingTime.get()
        }
    }

    fun logStats() {
        val count = operationCount.get()
        if (count > 0) {
            val avgTime = totalProcessingTime.get() / count
            Logger.logPerformance(
                "Stats - Operations: $count, Avg: ${avgTime}ms, Min: ${minProcessingTime.get()}ms, Max: ${maxProcessingTime.get()}ms"
            )
        }
    }
}
