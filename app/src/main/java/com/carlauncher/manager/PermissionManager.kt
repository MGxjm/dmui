package com.carlauncher.manager

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import com.carlauncher.data.prefs.PreferencesManager
import com.carlauncher.utils.Logger
import java.io.File

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
        return try {
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            val hasPermission = enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
            Logger.d(TAG, "hasAccessibilityPermission: $hasPermission")
            hasPermission
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to check accessibility permission", e)
            false
        }
    }

    fun hasFloatingWindowPermission(): Boolean {
        return try {
            val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
            Logger.d(TAG, "hasFloatingWindowPermission: $granted")
            granted
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to check floating window permission", e)
            false
        }
    }

    fun hasAdbPermission(): Boolean {
        return try {
            val adbEnabled = Settings.Global.getInt(context.contentResolver, "adb_enabled", 0) == 1
            Logger.d(TAG, "hasAdbPermission: $adbEnabled")
            adbEnabled
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to check ADB permission, using cached value", e)
            prefs.getSettings().adbEnabled
        }
    }

    fun isWirelessAdbEnabled(): Boolean {
        return try {
            val wirelessEnabled = Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) == 1
            Logger.d(TAG, "isWirelessAdbEnabled: $wirelessEnabled")
            wirelessEnabled
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to check wireless ADB status", e)
            false
        }
    }

    fun requestAccessibilityPermission() {
        Logger.d(TAG, "requestAccessibilityPermission called")
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            permissionCallback?.onPermissionResult(PERMISSION_ACCESSIBILITY, false, "请在无障碍设置中开启CarLauncher服务")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to open accessibility settings", e)
            permissionCallback?.onPermissionResult(PERMISSION_ACCESSIBILITY, false, "无法打开无障碍设置: ${e.message}")
        }
    }

    fun requestFloatingWindowPermission() {
        Logger.d(TAG, "requestFloatingWindowPermission called")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                permissionCallback?.onPermissionResult(PERMISSION_FLOATING_WINDOW, false, "请在设置中允许悬浮窗权限")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to open overlay permission settings", e)
            permissionCallback?.onPermissionResult(PERMISSION_FLOATING_WINDOW, false, "无法打开悬浮窗权限设置: ${e.message}")
        }
    }

    fun requestAdbPermission() {
        Logger.d(TAG, "requestAdbPermission called")
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            permissionCallback?.onPermissionResult(PERMISSION_ADB, false, "请在开发者选项中开启无线调试，并通过电脑配对授权")
            Logger.i(TAG, "Opened developer settings")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to open developer settings", e)
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(fallbackIntent)
                permissionCallback?.onPermissionResult(PERMISSION_ADB, false, "请手动进入设置 -> 开发者选项开启无线调试")
            } catch (e2: Exception) {
                Logger.e(TAG, "Failed to open settings", e2)
                permissionCallback?.onPermissionResult(PERMISSION_ADB, false, "无法打开设置，请手动进入开发者选项")
            }
        }
    }

    fun savePermissionState(permission: String, granted: Boolean) {
        Logger.d(TAG, "savePermissionState: $permission = $granted")
        when (permission) {
            PERMISSION_ADB -> prefs.saveSettings(prefs.getSettings().copy(adbEnabled = granted))
            PERMISSION_ACCESSIBILITY -> prefs.saveSettings(prefs.getSettings().copy(accessibilityEnabled = granted))
            PERMISSION_FLOATING_WINDOW -> prefs.saveSettings(prefs.getSettings().copy(floatingWindowEnabled = granted))
        }
    }

    fun getLogs(): String {
        return Logger.getLogContent()
    }

    fun clearLogs() {
        Logger.clearLogs()
    }

    companion object {
        private const val TAG = "PermissionManager"
        const val PERMISSION_ADB = "adb"
        const val PERMISSION_ACCESSIBILITY = "accessibility"
        const val PERMISSION_FLOATING_WINDOW = "floating_window"
    }
}
