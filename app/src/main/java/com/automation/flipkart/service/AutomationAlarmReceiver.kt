package com.automation.flipkart.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.automation.flipkart.utils.Logger

class AutomationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val serviceIntent = Intent(context, AutomationForegroundService::class.java)
        context.startForegroundService(serviceIntent)
        Logger.log("Automation Service Started by Alarm")
    }
}