package com.carlauncher.ui.home

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.carlauncher.R
import com.carlauncher.data.model.AppInfo
import com.carlauncher.ui.components.AppDrawer
import com.carlauncher.ui.components.ClockWeatherView
import com.carlauncher.ui.components.NavBarView
import com.carlauncher.ui.components.NotificationPanel
import com.carlauncher.ui.components.QuickAppsView
import com.carlauncher.ui.components.StatusBarView
import com.carlauncher.ui.settings.SettingsActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var statusBar: StatusBarView
    private lateinit var clockWeather: ClockWeatherView
    private lateinit var quickApps: QuickAppsView
    private lateinit var navBar: NavBarView
    private lateinit var appDrawer: AppDrawer
    private lateinit var notificationPanel: NotificationPanel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        initViewModel()
        initViews()
        bindViews()
        setupClickListeners()
        updateSystemStatus()
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
    }

    private fun initViews() {
        statusBar = findViewById(R.id.status_bar)
        clockWeather = findViewById(R.id.clock_weather)
        quickApps = findViewById(R.id.quick_apps)
        navBar = findViewById(R.id.nav_bar)
        appDrawer = findViewById(R.id.app_drawer)
        notificationPanel = findViewById(R.id.notification_panel)
        
        appDrawer.hide()
        notificationPanel.hide()
    }

    private fun bindViews() {
        statusBar.bindTime(viewModel.currentTime, this)
        statusBar.bindDate(viewModel.currentDate, this)
        statusBar.bindBatteryLevel(viewModel.batteryLevel, this)
        statusBar.bindWifiConnected(viewModel.isWifiConnected, this)
        statusBar.bindBluetoothConnected(viewModel.isBluetoothConnected, this)
        
        clockWeather.bindTime(viewModel.currentTime, this)
        clockWeather.bindDate(viewModel.currentDate, this)
        clockWeather.setWeather("26°C", "晴天")
        
        quickApps.bindApps(viewModel.favoriteApps, this)
        appDrawer.bindApps(viewModel.apps, this)
    }

    private fun setupClickListeners() {
        quickApps.onAppClick = { app -> launchApp(app) }
        
        navBar.onBackClick = {
            try {
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        navBar.onHomeClick = {
        }
        
        navBar.onRecentClick = {
            try {
                val intent = Intent("android.intent.action.RECENT")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        navBar.onDrawerClick = { toggleDrawer() }
        
        appDrawer.onClose = { closeDrawer() }
        appDrawer.onAppClick = { app ->
            launchApp(app)
            closeDrawer()
        }
        
        notificationPanel.onClose = { closeNotificationPanel() }
        
        findViewById<View>(R.id.settings_button).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun launchApp(app: AppInfo) {
        viewModel.launchApp(app)
    }

    private fun toggleDrawer() {
        if (appDrawer.isVisible()) {
            closeDrawer()
        } else {
            openDrawer()
        }
    }

    private fun openDrawer() {
        appDrawer.show()
        notificationPanel.hide()
    }

    private fun closeDrawer() {
        appDrawer.hide()
    }

    private fun closeNotificationPanel() {
        notificationPanel.hide()
    }

    private fun updateSystemStatus() {
        viewModel.updateBatteryLevel(this)
        viewModel.updateWifiStatus(this)
        viewModel.updateBluetoothStatus(this)
    }

    override fun onBackPressed() {
        if (appDrawer.isVisible()) {
            closeDrawer()
        } else {
            super.onBackPressed()
        }
    }
}
