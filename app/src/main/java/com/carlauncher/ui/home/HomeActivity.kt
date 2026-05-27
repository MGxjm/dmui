package com.carlauncher.ui.home

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.carlauncher.R
import com.carlauncher.data.model.AppInfo
import com.carlauncher.manager.AdbManager
import com.carlauncher.manager.AdbManagerHolder
import com.carlauncher.manager.PermissionManager
import com.carlauncher.service.AdbConnectionService
import com.carlauncher.service.CarAccessibilityService
import com.carlauncher.service.FloatingMapService
import com.carlauncher.ui.components.AppDrawer
import com.carlauncher.ui.components.CarControlPanel
import com.carlauncher.ui.components.ClockWeatherView
import com.carlauncher.ui.components.DisplayControlPanel
import com.carlauncher.ui.components.NotificationPanel
import com.carlauncher.ui.components.QuickAppsView
import com.carlauncher.ui.components.StatusBarView
import com.carlauncher.ui.settings.PermissionSettingsActivity
import com.carlauncher.ui.settings.SettingsActivity
import com.carlauncher.utils.AnimationUtils
import com.carlauncher.utils.Logger
import com.google.android.material.button.MaterialButton

class HomeActivity : AppCompatActivity() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var statusBar: StatusBarView
    private lateinit var clockWeather: ClockWeatherView
    private lateinit var quickApps: QuickAppsView
    private lateinit var appDrawer: AppDrawer
    private lateinit var notificationPanel: NotificationPanel
    private lateinit var carControlPanel: CarControlPanel
    private lateinit var displayControlPanel: DisplayControlPanel
    private lateinit var contentContainer: View
    private lateinit var mainContent: View
    private lateinit var dockBar: View
    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.init(this)
        Logger.d("HomeActivity", "onCreate")
        setContentView(R.layout.activity_home)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        initViewModel()
        initViews()
        bindViews()
        setupDockListeners()
        updateSystemStatus()
        checkPermissions()
        startAdbConnectionService()

        AnimationUtils.fadeIn(mainContent, 400)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
    }

    private fun initViews() {
        statusBar = findViewById(R.id.status_bar)
        clockWeather = findViewById(R.id.clock_weather)
        quickApps = findViewById(R.id.quick_apps)
        appDrawer = findViewById(R.id.app_drawer)
        notificationPanel = findViewById(R.id.notification_panel)
        carControlPanel = findViewById(R.id.car_control_panel)
        displayControlPanel = findViewById(R.id.display_control_panel)
        contentContainer = findViewById(R.id.content_container)
        mainContent = findViewById(R.id.main_content)
        dockBar = findViewById(R.id.dock_bar)

        contentContainer.visibility = View.GONE
    }

    private fun bindViews() {
        statusBar.bindTime(viewModel.currentTime, this)
        statusBar.bindDate(viewModel.currentDate, this)
        statusBar.bindBatteryLevel(viewModel.batteryLevel, this)
        statusBar.bindWifiConnected(viewModel.isWifiConnected, this)
        statusBar.bindBluetoothConnected(viewModel.isBluetoothConnected, this)

        clockWeather.bindTime(viewModel.currentTime, this)
        clockWeather.bindDate(viewModel.currentDate, this)
        clockWeather.setWeather("--", "--")

        quickApps.bindApps(viewModel.favoriteApps, this)
        appDrawer.bindApps(viewModel.apps, this)
    }

    private fun setupDockListeners() {
        // 导航区
        findViewById<MaterialButton>(R.id.dock_back).setOnClickListener {
            AnimationUtils.buttonPressAnimation(it)
            performBack()
        }

        findViewById<MaterialButton>(R.id.dock_home).setOnClickListener {
            AnimationUtils.buttonPressAnimation(it)
            performHome()
        }

        findViewById<MaterialButton>(R.id.dock_recent).setOnClickListener {
            AnimationUtils.buttonPressAnimation(it)
            performRecent()
        }

        // 功能区
        findViewById<MaterialButton>(R.id.dock_drawer).setOnClickListener {
            AnimationUtils.buttonPressAnimation(it)
            if (appDrawer.isVisible()) closeDrawer() else openDrawer()
        }

        findViewById<MaterialButton>(R.id.dock_car_control).setOnClickListener {
            AnimationUtils.buttonPressAnimation(it)
            if (carControlPanel.visibility == View.VISIBLE) closePanel(carControlPanel) else openCarControlPanel()
        }

        findViewById<MaterialButton>(R.id.dock_display).setOnClickListener {
            AnimationUtils.buttonPressAnimation(it)
            if (displayControlPanel.visibility == View.VISIBLE) closePanel(displayControlPanel) else openDisplayControlPanel()
        }

        findViewById<MaterialButton>(R.id.dock_floating_map).setOnClickListener {
            AnimationUtils.buttonPressAnimation(it)
            startFloatingMapService()
        }

        // 系统区
        findViewById<MaterialButton>(R.id.dock_notification).setOnClickListener {
            AnimationUtils.buttonPressAnimation(it)
            if (notificationPanel.isVisible()) closeNotificationPanel() else openNotificationPanel()
        }

        findViewById<MaterialButton>(R.id.dock_settings).setOnClickListener {
            AnimationUtils.buttonPressAnimation(it)
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 应用点击
        quickApps.onAppClick = { app ->
            AnimationUtils.buttonPressAnimation(quickApps)
            launchApp(app)
        }

        appDrawer.onClose = { closeDrawer() }
        appDrawer.onAppClick = { app ->
            AnimationUtils.buttonPressAnimation(appDrawer)
            launchApp(app)
            closeDrawer()
        }

        notificationPanel.onClose = { closeNotificationPanel() }
    }

    private fun performBack() {
        try {
            CarAccessibilityService.instance?.performGlobalBack()
                ?: onBackPressedDispatcher.onBackPressed()
        } catch (e: Exception) {
            Logger.e("HomeActivity", "Back failed", e)
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun performHome() {
        try {
            CarAccessibilityService.instance?.performGlobalHome() ?: run {
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        } catch (e: Exception) {
            Logger.e("HomeActivity", "Home failed", e)
        }
    }

    private fun performRecent() {
        try {
            CarAccessibilityService.instance?.performGlobalRecent() ?: run {
                Thread { Runtime.getRuntime().exec("input keyevent 187").waitFor() }.start()
            }
        } catch (e: Exception) {
            Logger.e("HomeActivity", "Recent failed", e)
            Toast.makeText(this, "需要无障碍权限", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchApp(app: AppInfo) {
        viewModel.launchApp(app)
    }

    private fun openDrawer() {
        hideAllPanels()
        contentContainer.visibility = View.VISIBLE
        appDrawer.show()
        AnimationUtils.scaleInAnimation(appDrawer)
        AnimationUtils.backdropFadeIn(mainContent)
    }

    private fun closeDrawer() {
        AnimationUtils.scaleOutAnimation(appDrawer) {
            contentContainer.visibility = View.GONE
            AnimationUtils.backdropFadeOut(mainContent)
        }
    }

    private fun openNotificationPanel() {
        hideAllPanels()
        contentContainer.visibility = View.VISIBLE
        notificationPanel.show()
        AnimationUtils.slideInFromBottom(notificationPanel)
        AnimationUtils.backdropFadeIn(mainContent)
    }

    private fun closeNotificationPanel() {
        AnimationUtils.scaleOutAnimation(notificationPanel) {
            contentContainer.visibility = View.GONE
            AnimationUtils.backdropFadeOut(mainContent)
        }
    }

    private fun openCarControlPanel() {
        hideAllPanels()
        contentContainer.visibility = View.VISIBLE
        carControlPanel.visibility = View.VISIBLE
        AnimationUtils.scaleInAnimation(carControlPanel)
        AnimationUtils.backdropFadeIn(mainContent)
    }

    private fun openDisplayControlPanel() {
        hideAllPanels()
        contentContainer.visibility = View.VISIBLE
        displayControlPanel.visibility = View.VISIBLE
        AnimationUtils.scaleInAnimation(displayControlPanel)
        AnimationUtils.backdropFadeIn(mainContent)
    }

    private fun closePanel(panel: View) {
        AnimationUtils.scaleOutAnimation(panel) {
            contentContainer.visibility = View.GONE
            AnimationUtils.backdropFadeOut(mainContent)
        }
    }

    private fun hideAllPanels() {
        appDrawer.hide()
        notificationPanel.hide()
        carControlPanel.visibility = View.GONE
        displayControlPanel.visibility = View.GONE
    }

    private fun startFloatingMapService() {
        startService(Intent(this, FloatingMapService::class.java))
    }

    private fun startAdbConnectionService() {
        val adbManager = AdbManagerHolder.get(this)
        if (adbManager.isConnected()) {
            Logger.d("HomeActivity", "ADB already connected, skip auto-connect")
            return
        }
        if (!adbManager.shouldAutoConnect()) {
            Logger.d("HomeActivity", "ADB connect already attempted, skip auto-connect")
            return
        }
        val intent = Intent(this, AdbConnectionService::class.java).apply {
            action = AdbConnectionService.ACTION_CONNECT
        }
        startService(intent)
    }

    private fun updateSystemStatus() {
        viewModel.updateBatteryLevel(this)
        viewModel.updateWifiStatus(this)
        viewModel.updateBluetoothStatus(this)
    }

    private fun checkPermissions() {
        permissionManager = PermissionManager(this)
        permissionManager.setPermissionCallback(object : PermissionManager.PermissionCallback {
            override fun onPermissionResult(permission: String, granted: Boolean, message: String) {
                runOnUiThread {
                    Toast.makeText(this@HomeActivity, message, Toast.LENGTH_LONG).show()
                }
            }
        })

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "需要悬浮窗权限才能显示浮动地图", Toast.LENGTH_LONG).show()
            permissionManager.requestFloatingWindowPermission()
        }

        if (!permissionManager.hasAccessibilityPermission()) {
            Toast.makeText(this, "建议开启无障碍权限以使用返回和最近任务功能", Toast.LENGTH_LONG).show()
        }

        Logger.d("HomeActivity", "Permissions: overlay=${Settings.canDrawOverlays(this)}, accessibility=${permissionManager.hasAccessibilityPermission()}")
    }

    override fun onBackPressed() {
        when {
            appDrawer.isVisible() -> closeDrawer()
            carControlPanel.visibility == View.VISIBLE -> closePanel(carControlPanel)
            displayControlPanel.visibility == View.VISIBLE -> closePanel(displayControlPanel)
            notificationPanel.isVisible() -> closeNotificationPanel()
            else -> super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        updateSystemStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, FloatingMapService::class.java))
        stopService(Intent(this, AdbConnectionService::class.java))
    }
}
