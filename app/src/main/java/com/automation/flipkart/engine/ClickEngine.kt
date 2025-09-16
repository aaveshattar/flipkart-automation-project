package com.automation.flipkart.engine
 
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.automation.flipkart.config.UltraConfig
import com.automation.flipkart.utils.UltraLogger
import kotlinx.coroutines.*
 
class ClickEngine(private val service: AccessibilityService) {
    
    suspend fun performUltraFastClick(
        node: AccessibilityNodeInfo,
        bounds: Rect,
        pattern: UltraConfig.ButtonPattern,
        strategy: TrafficAnalyzer.Strategy
    ): Boolean = withContext(Dispatchers.Main) {
        
        val startTime = System.currentTimeMillis()
        var success = false
        
        try {
            // Method 1: Direct click action (fastest)
            success = performDirectClick(node, pattern, strategy)
            
            if (!success && strategy.clickRetries > 1) {
                // Method 2: Gesture-based click (more reliable)
                success = performGestureClick(bounds, strategy)
            }
            
            if (success) {
                val clickTime = System.currentTimeMillis() - startTime
                UltraLogger.log("✅ CLICK SUCCESS: ${pattern.text} in ${clickTime}ms")
                
                // Double-click for critical buttons in emergency mode
                if (strategy == TrafficAnalyzer.Strategy.EMERGENCY && pattern.isCritical) {
                    delay(10L)
                    performDirectClick(node, pattern, strategy)
                }
            }
            
        } catch (e: Exception) {
            UltraLogger.logError("Click engine failed for ${pattern.text}", e)
        }
        
        success
    }
    
    private suspend fun performDirectClick(
        node: AccessibilityNodeInfo,
        pattern: UltraConfig.ButtonPattern,
        strategy: TrafficAnalyzer.Strategy
    ): Boolean = withContext(Dispatchers.Main) {
        
        try {
            repeat(strategy.clickRetries) { attempt ->
                if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return@withContext true
                }
                
                // Brief delay between retries
                if (attempt < strategy.clickRetries - 1) {
                    delay(strategy.gestureSpeed.toLong())
                }
            }
            
        } catch (e: Exception) {
            UltraLogger.logError("Direct click failed", e)
        }
        
        false
    }
    
    private suspend fun performGestureClick(
        bounds: Rect,
        strategy: TrafficAnalyzer.Strategy
    ): Boolean = withContext(Dispatchers.Main) {
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return@withContext false
        }
        
        try {
            val centerX = bounds.centerX().toFloat()
            val centerY = bounds.centerY().toFloat()
            
            val path = Path().apply {
                moveTo(centerX, centerY)
            }
            
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(
                    path, 
                    0, 
                    strategy.gestureSpeed.toLong()
                ))
                .build()
            
            val result = CompletableDeferred<Boolean>()
            
            service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    result.complete(true)
                }
                
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    result.complete(false)
                }
            }, null)
            
            return@withContext withTimeoutOrNull(500L) { result.await() } ?: false
            
        } catch (e: Exception) {
            UltraLogger.logError("Gesture click failed", e)
            return@withContext false
        }
    }
}