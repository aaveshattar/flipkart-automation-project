package com.automation.flipkart.service
 
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.automation.flipkart.utils.UltraLogger
 
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            UltraLogger.log("Boot completed - starting ultra fast service")
            
            val serviceIntent = Intent(context, UltraFastForegroundService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}