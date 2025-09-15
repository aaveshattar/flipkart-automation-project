package com.automation.flipkart.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.automation.flipkart.config.AutomationConfig
import com.automation.flipkart.utils.Logger

class AutomationForegroundService : Service() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        startAutoRefresh()
        return START_STICKY
    }

    private fun startAutoRefresh() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                Logger.log("Refreshing Flipkart App")
                val launchIntent = packageManager.getLaunchIntentForPackage("com.flipkart.android")
                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                handler.postDelayed(this, AutomationConfig.REFRESH_INTERVAL_MS)
            }
        }, AutomationConfig.REFRESH_INTERVAL_MS)
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, "automation_channel")
            .setContentTitle("Flipkart Automation Running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}