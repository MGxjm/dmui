package com.carlauncher.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.carlauncher.ui.home.HomeActivity

class CarAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "CarAccessibilityService"
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        serviceInfo = info
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString()
                Log.d(TAG, "Window state changed: $packageName")
                // 可以在这里处理应用启动事件
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // 窗口内容变化
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }
    
    fun performGlobalBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }
    
    fun performGlobalHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }
    
    fun performGlobalRecent() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
    }
    
    fun performGlobalPowerDialog() {
        performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
    }
    
    fun performGlobalNotification() {
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }
    
    fun performGlobalQuickSettings() {
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
    }
}
