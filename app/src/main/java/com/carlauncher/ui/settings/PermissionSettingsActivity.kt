package com.carlauncher.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.carlauncher.R
import com.carlauncher.manager.AdbManager
import com.carlauncher.manager.AdbManagerHolder
import com.carlauncher.manager.PermissionManager
import com.carlauncher.service.AdbConnectionService
import com.carlauncher.utils.AnimationUtils
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PermissionSettingsActivity : AppCompatActivity(), PermissionManager.PermissionCallback {

    private lateinit var permissionManager: PermissionManager
    private lateinit var adbManager: AdbManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var adbStatusText: TextView
    private lateinit var accessibilityStatusText: TextView
    private lateinit var floatingWindowStatusText: TextView
    private lateinit var connectAdbButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_settings)

        permissionManager = PermissionManager(this)
        permissionManager.setPermissionCallback(this)
        adbManager = AdbManagerHolder.get(this)

        initViews()
        updatePermissionStatus()
        setupClickListeners()
    }

    private fun initViews() {
        adbStatusText = findViewById(R.id.adb_status_text)
        accessibilityStatusText = findViewById(R.id.accessibility_status_text)
        floatingWindowStatusText = findViewById(R.id.floating_window_status_text)
        connectAdbButton = findViewById(R.id.connect_adb_button)
    }

    private fun updatePermissionStatus() {
        val adbEnabled = permissionManager.hasAdbPermission()
        val adbConnected = adbManager.isConnected()
        val accessibilityGranted = permissionManager.hasAccessibilityPermission()
        val floatingWindowGranted = permissionManager.hasFloatingWindowPermission()

        adbStatusText.text = when {
            adbConnected -> "已连接"
            adbEnabled -> "已开启/未连接"
            else -> "未开启"
        }
        adbStatusText.setTextColor(getColor(if (adbConnected) R.color.success else R.color.error))

        connectAdbButton.text = if (adbConnected) "断开 ADB" else "连接 ADB"

        accessibilityStatusText.text = if (accessibilityGranted) "已授权" else "未授权"
        accessibilityStatusText.setTextColor(getColor(if (accessibilityGranted) R.color.success else R.color.error))

        floatingWindowStatusText.text = if (floatingWindowGranted) "已授权" else "未授权"
        floatingWindowStatusText.setTextColor(getColor(if (floatingWindowGranted) R.color.success else R.color.error))
    }

    private fun setupClickListeners() {
        findViewById<MaterialButton>(R.id.request_adb_button).setOnClickListener {
            AnimationUtils.buttonPressAnimation(it)
            permissionManager.requestAdbPermission()
        }

        connectAdbButton.setOnClickListener {
            AnimationUtils.buttonPressAnimation(it)
            if (adbManager.isConnected()) {
                adbManager.disconnect()
                Toast.makeText(this, "ADB已断开", Toast.LENGTH_SHORT).show()
                updatePermissionStatus()
            } else {
                connectAdbButton.isEnabled = false
                connectAdbButton.text = "连接中..."
                scope.launch {
                    val intent = Intent(this@PermissionSettingsActivity, AdbConnectionService::class.java).apply {
                        action = AdbConnectionService.ACTION_CONNECT
                    }
                    startService(intent)
                    Toast.makeText(this@PermissionSettingsActivity, "正在连接ADB...", Toast.LENGTH_SHORT).show()
                    Thread.sleep(3000)
                    runOnUiThread {
                        connectAdbButton.isEnabled = true
                        updatePermissionStatus()
                    }
                }
            }
        }

        findViewById<MaterialButton>(R.id.request_accessibility_button).setOnClickListener {
            AnimationUtils.buttonPressAnimation(it)
            permissionManager.requestAccessibilityPermission()
        }

        findViewById<MaterialButton>(R.id.request_floating_window_button).setOnClickListener {
            AnimationUtils.buttonPressAnimation(it)
            permissionManager.requestFloatingWindowPermission()
        }
    }

    override fun onPermissionResult(permission: String, granted: Boolean, message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }
}
