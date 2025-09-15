package com.automation.flipkart.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.automation.flipkart.config.AutomationConfig
import com.automation.flipkart.utils.Logger
import com.automation.flipkart.utils.PerformanceMonitor
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class OptimizedFlipkartAutomationService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isProcessing = AtomicBoolean(false)
    private val nodeCache = ConcurrentHashMap<String, AccessibilityNodeInfo>()
    private val clickTargets = arrayOf("Buy Now", "Add to Cart", "Continue", "Pay Now", "Proceed to Pay")
    private val performanceMonitor = PerformanceMonitor()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!shouldProcessEvent(event)) return
        
        val rootNode = rootInActiveWindow ?: return
        
        if (!isProcessing.compareAndSet(false, true)) {
            Logger.logWarning("Skipping event - already processing")
            return
        }

        try {
            processEventOptimized(rootNode)
        } finally {
            isProcessing.set(false)
        }
    }

    private fun shouldProcessEvent(event: AccessibilityEvent?): Boolean {
        return event != null && 
               event.packageName == "com.flipkart.android" &&
               (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
    }

    private fun processEventOptimized(rootNode: AccessibilityNodeInfo) {
        serviceScope.launch {
            val startTime = System.currentTimeMillis()
            
            try {
                clickTargets.forEach { buttonText ->
                    async(Dispatchers.IO) {
                        processClickTarget(rootNode, buttonText)
                    }
                }
                
            } catch (e: Exception) {
                Logger.logError("Error in processEventOptimized", e)
            } finally {
                val processingTime = System.currentTimeMillis() - startTime
                performanceMonitor.recordProcessingTime(processingTime)
            }
        }
    }

    private suspend fun processClickTarget(root: AccessibilityNodeInfo, buttonText: String) {
        withContext(Dispatchers.Main) {
            repeat(AutomationConfig.MAX_RETRY_ATTEMPTS) { attempt ->
                val success = findAndClickButton(root, buttonText)
                if (success) return@withContext
                delay(50L * (attempt + 1))
            }
        }
    }

    private fun findAndClickButton(root: AccessibilityNodeInfo, buttonText: String): Boolean {
        val nodes = root.findAccessibilityNodeInfosByText(buttonText)
        
        nodes?.forEach { node ->
            if (node.isClickable && node.isEnabled && node.isVisibleToUser) {
                return performOptimizedClick(node, buttonText)
            }
        }
        
        return false
    }

    private fun performOptimizedClick(node: AccessibilityNodeInfo, buttonText: String): Boolean {
        return try {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            
            if (bounds.width() <= 0 || bounds.height() <= 0) {
                Logger.logWarning("Invalid bounds for $buttonText")
                return false
            }

            val success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            
            if (success) {
                Logger.logSuccess("Successfully clicked $buttonText")
                nodeCache.clear()
            }
            
            success
        } catch (e: Exception) {
            Logger.logError("Error clicking $buttonText", e)
            false
        }
    }

    override fun onInterrupt() {
        Logger.logWarning("Service interrupted")
        cleanup()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    private fun cleanup() {
        serviceScope.cancel()
        nodeCache.clear()
        performanceMonitor.logStats()
    }
}