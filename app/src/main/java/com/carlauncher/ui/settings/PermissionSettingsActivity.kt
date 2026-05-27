package com.carlauncher.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.carlauncher.R
import com.carlauncher.manager.PermissionManager
import com.google.android.material.snackbar.Snackbar

class PermissionSettingsActivity : AppCompatActivity(), PermissionManager.PermissionCallback {

    private lateinit var permissionManager: PermissionManager
    
    private lateinit var adbStatusText: TextView
    private lateinit var accessibilityStatusText: TextView
    private lateinit var floatingWindowStatusText: TextView
    
    private lateinit var requestAdbButton: Button
    private lateinit var requestAccessibilityButton: Button
    private lateinit var requestFloatingWindowButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_settings)
        
        permissionManager = PermissionManager(this)
        permissionManager.setPermissionCallback(this)
        initViews()
        updatePermissionStatus()
        setupClickListeners()
    }

    private fun initViews() {
        adbStatusText = findViewById(R.id.adb_status_text)
        accessibilityStatusText = findViewById(R.id.accessibility_status_text)
        floatingWindowStatusText = findViewById(R.id.floating_window_status_text)
        
        requestAdbButton = findViewById(R.id.request_adb_button)
        requestAccessibilityButton = findViewById(R.id.request_accessibility_button)
        requestFloatingWindowButton = findViewById(R.id.request_floating_window_button)
    }

    private fun updatePermissionStatus() {
        val adbGranted = permissionManager.hasAdbPermission()
        val accessibilityGranted = permissionManager.hasAccessibilityPermission()
        val floatingWindowGranted = permissionManager.hasFloatingWindowPermission()
        
        adbStatusText.text = if (adbGranted) "已授权" else "未授权"
        adbStatusText.setTextColor(if (adbGranted) getColor(R.color.success) else getColor(R.color.error))
        
        accessibilityStatusText.text = if (accessibilityGranted) "已授权" else "未授权"
        accessibilityStatusText.setTextColor(if (accessibilityGranted) getColor(R.color.success) else getColor(R.color.error))
        
        floatingWindowStatusText.text = if (floatingWindowGranted) "已授权" else "未授权"
        floatingWindowStatusText.setTextColor(if (floatingWindowGranted) getColor(R.color.success) else getColor(R.color.error))
    }

    private fun setupClickListeners() {
        requestAdbButton.setOnClickListener {
            showSnackbar("正在打开开发者选项，请开启无线调试")
            permissionManager.requestAdbPermission()
        }
        
        requestAccessibilityButton.setOnClickListener {
            permissionManager.requestAccessibilityPermission()
        }
        
        requestFloatingWindowButton.setOnClickListener {
            permissionManager.requestFloatingWindowPermission()
        }
    }

    override fun onPermissionResult(permission: String, granted: Boolean, message: String) {
        showSnackbar(message)
    }
    
    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }
}