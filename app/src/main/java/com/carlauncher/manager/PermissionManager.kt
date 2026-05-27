package com.carlauncher.manager

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.carlauncher.data.prefs.PreferencesManager

class PermissionManager(private val context: Context) {
    
    private val prefs = PreferencesManager(context)
    private var permissionCallback: PermissionCallback? = null
    
    interface PermissionCallback {
        fun onPermissionResult(permission: String, granted: Boolean, message: String)
    }
    
    fun setPermissionCallback(callback: PermissionCallback) {
        this.permissionCallback = callback
    }
    
    fun hasAccessibilityPermission(): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
    }
    
    fun hasFloatingWindowPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                android.os.Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        }
    }
    
    fun hasAdbPermission(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "adb_enabled", 0) == 1
        } catch (e: Exception) {
            prefs.getSettings().adbEnabled
        }
    }
    
    fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    fun requestFloatingWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }
    
    fun requestAdbPermission() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            permissionCallback?.onPermissionResult(PERMISSION_ADB, false, "请在开发者选项中开启无线调试，然后点击\"配对设备\"并输入配对码")
        } catch (e: Exception) {
            fallbackToDeveloperSettings()
        }
    }
    
    private fun fallbackToDeveloperSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        permissionCallback?.onPermissionResult(PERMISSION_ADB, false, "请在开发者选项中开启无线调试功能")
    }
    
    fun savePermissionState(permission: String, granted: Boolean) {
        when (permission) {
            PERMISSION_ADB -> prefs.saveSettings(prefs.getSettings().copy(adbEnabled = granted))
            PERMISSION_ACCESSIBILITY -> prefs.saveSettings(prefs.getSettings().copy(accessibilityEnabled = granted))
            PERMISSION_FLOATING_WINDOW -> prefs.saveSettings(prefs.getSettings().copy(floatingWindowEnabled = granted))
        }
    }
    
    companion object {
        const val PERMISSION_ADB = "adb"
        const val PERMISSION_ACCESSIBILITY = "accessibility"
        const val PERMISSION_FLOATING_WINDOW = "floating_window"
    }
}