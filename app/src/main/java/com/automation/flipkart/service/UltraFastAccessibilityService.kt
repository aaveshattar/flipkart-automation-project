package com.automation.flipkart.service
 
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.automation.flipkart.config.UltraConfig
import com.automation.flipkart.engine.ClickEngine
import com.automation.flipkart.engine.TrafficAnalyzer
import com.automation.flipkart.utils.UltraLogger
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
 
class UltraFastAccessibilityService : AccessibilityService() {
    
    private val isProcessing = AtomicBoolean(false)
    private val lastEventTime = AtomicLong(0)
    private val backgroundThread = HandlerThread("UltraFast-BG").apply { start() }
    private val backgroundHandler = Handler(backgroundThread.looper)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private lateinit var clickEngine: ClickEngine
    private lateinit var trafficAnalyzer: TrafficAnalyzer
    
    // Cache for ultra-fast lookups
    private val nodeCache = ConcurrentHashMap<String, CachedNodeInfo>()
    private val buttonPatterns = UltraConfig.ULTRA_FAST_PATTERNS
    
    data class CachedNodeInfo(
        val node: AccessibilityNodeInfo,
        val timestamp: Long,
        val bounds: Rect
    )
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        initializeUltraFastEngine()
        UltraLogger.log("🚀 ULTRA FAST SERVICE CONNECTED - Android ${Build.VERSION.SDK_INT}")
    }
    
    private fun initializeUltraFastEngine() {
        clickEngine = ClickEngine(this)
        trafficAnalyzer = TrafficAnalyzer()
        
        // Preload critical patterns for instant recognition
        preloadButtonPatterns()
        
        // Start traffic monitoring
        trafficAnalyzer.startMonitoring()
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isFlipkartEvent(event)) return
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEventTime.get() < UltraConfig.MIN_EVENT_INTERVAL) return
        
        if (!isProcessing.compareAndSet(false, true)) {
            UltraLogger.log("⚡ SKIPPED - Already processing")
            return
        }
        
        lastEventTime.set(currentTime)
        
        // Ultra-fast background processing
        backgroundHandler.post {
            processEventUltraFast(event!!)
        }
    }
    
    private fun processEventUltraFast(event: AccessibilityEvent) {
        serviceScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val rootNode = rootInActiveWindow
                
                if (rootNode == null) {
                    UltraLogger.log("❌ No root node available")
                    return@launch
                }
                
                // Get current traffic strategy
                val strategy = trafficAnalyzer.getCurrentStrategy()
                UltraLogger.log("🎯 Using strategy: ${strategy.name}")
                
                // Execute ultra-fast click detection
                val clickResults = executeUltraFastClicks(rootNode, strategy)
                
                val processingTime = System.currentTimeMillis() - startTime
                UltraLogger.logPerformance("Total processing: ${processingTime}ms")
                
                trafficAnalyzer.recordPerformance(processingTime, clickResults.successCount)
                
            } catch (e: Exception) {
                UltraLogger.logError("Processing failed", e)
                trafficAnalyzer.recordFailure()
            } finally {
                isProcessing.set(false)
            }
        }
    }
    
    private suspend fun executeUltraFastClicks(
        root: AccessibilityNodeInfo, 
        strategy: TrafficAnalyzer.Strategy
    ): ClickResults = withContext(Dispatchers.Default) {
        
        val results = ClickResults()
        val jobs = mutableListOf<Deferred<ClickResult>>()
        
        // Create coroutines for each button pattern
        buttonPatterns.forEach { pattern ->
            val job = async {
                findAndClickUltraFast(root, pattern, strategy)
            }
            jobs.add(job)
        }
        
        // Wait for completion with timeout
        try {
            withTimeout(strategy.maxProcessingTime) {
                val clickResults = jobs.awaitAll()
                results.successCount = clickResults.count { it.success }
                results.totalAttempts = clickResults.size
            }
        } catch (e: TimeoutCancellationException) {
            UltraLogger.log("⏱️ Processing timeout - cancelling remaining tasks")
            jobs.forEach { it.cancel() }
        }
        
        results
    }
    
    private suspend fun findAndClickUltraFast(
        root: AccessibilityNodeInfo,
        pattern: UltraConfig.ButtonPattern,
        strategy: TrafficAnalyzer.Strategy
    ): ClickResult = withContext(Dispatchers.Default) {
        
        val result = ClickResult(pattern.text)
        
        try {
            // Check cache first for ultra-fast lookup
            val cacheKey = "${pattern.text}_${strategy.name}"
            val cachedNode = nodeCache[cacheKey]
            
            if (cachedNode != null && 
                System.currentTimeMillis() - cachedNode.timestamp < UltraConfig.CACHE_TTL) {
                
                // Use cached node for instant click
                result.success = clickEngine.performUltraFastClick(
                    cachedNode.node, 
                    cachedNode.bounds, 
                    pattern,
                    strategy
                )
                
                if (result.success) {
                    UltraLogger.log("⚡ CACHE HIT: ${pattern.text}")
                    return@withContext result
                }
            }
            
            // Fallback to fresh search with multiple methods
            result.success = searchAndClickMultiMethod(root, pattern, strategy, cacheKey)
            
        } catch (e: Exception) {
            UltraLogger.logError("Click failed for ${pattern.text}", e)
        }
        
        result
    }
    
    private suspend fun searchAndClickMultiMethod(
        root: AccessibilityNodeInfo,
        pattern: UltraConfig.ButtonPattern,
        strategy: TrafficAnalyzer.Strategy,
        cacheKey: String
    ): Boolean = withContext(Dispatchers.Default) {
        
        // Method 1: Text-based search (fastest)
        var nodes = withTimeoutOrNull(strategy.searchTimeout) {
            root.findAccessibilityNodeInfosByText(pattern.text)
        } ?: emptyList()
        
        for (node in nodes) {
            if (isValidClickableNode(node)) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                
                // Cache for future use
                nodeCache[cacheKey] = CachedNodeInfo(node, System.currentTimeMillis(), bounds)
                
                if (clickEngine.performUltraFastClick(node, bounds, pattern, strategy)) {
                    return@withContext true
                }
            }
        }
        
        // Method 2: Resource ID search (backup)
        pattern.resourceIds.forEach { resourceId ->
            nodes = root.findAccessibilityNodeInfosByViewId(resourceId) ?: emptyList()
            
            for (node in nodes) {
                if (isValidClickableNode(node)) {
                    val bounds = Rect()
                    node.getBoundsInScreen(bounds)
                    
                    if (clickEngine.performUltraFastClick(node, bounds, pattern, strategy)) {
                        return@withContext true
                    }
                }
            }
        }
        
        // Method 3: Content description search (last resort)
        pattern.contentDescriptions.forEach { desc ->
            nodes = root.findAccessibilityNodeInfosByText(desc) ?: emptyList()
            
            for (node in nodes) {
                if (isValidClickableNode(node)) {
                    val bounds = Rect()
                    node.getBoundsInScreen(bounds)
                    
                    if (clickEngine.performUltraFastClick(node, bounds, pattern, strategy)) {
                        return@withContext true
                    }
                }
            }
        }
        
        false
    }
    
    private fun isValidClickableNode(node: AccessibilityNodeInfo?): Boolean {
        return node != null && 
               node.isClickable && 
               node.isEnabled && 
               node.isVisibleToUser &&
               !node.bounds.isEmpty
    }
    
    private fun isFlipkartEvent(event: AccessibilityEvent?): Boolean {
        return event?.packageName == "com.flipkart.android" &&
               (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED)
    }
    
    private fun preloadButtonPatterns() {
        // Precompile regex patterns for ultra-fast matching
        buttonPatterns.forEach { pattern ->
            pattern.compileRegex()
        }
    }
    
    override fun onInterrupt() {
        cleanup()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }
    
    private fun cleanup() {
        serviceScope.cancel()
        backgroundThread.quitSafely()
        nodeCache.clear()
        trafficAnalyzer.stop()
        UltraLogger.log("🛑 ULTRA FAST SERVICE DESTROYED")
    }
    
    data class ClickResults(
        var successCount: Int = 0,
        var totalAttempts: Int = 0
    )
    
    data class ClickResult(
        val buttonText: String,
        var success: Boolean = false,
        var processingTime: Long = 0
    )
}