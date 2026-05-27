package com.carlauncher.ui.home

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.carlauncher.data.AppRepository
import com.carlauncher.data.model.AppInfo
import com.carlauncher.data.model.NotificationInfo
import kotlinx.coroutines.launch
import java.util.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val appRepository = AppRepository(application)
    
    private val _apps = MutableLiveData<List<AppInfo>>()
    val apps: LiveData<List<AppInfo>> = _apps
    
    private val _favoriteApps = MutableLiveData<List<AppInfo>>()
    val favoriteApps: LiveData<List<AppInfo>> = _favoriteApps
    
    private val _notifications = MutableLiveData<List<NotificationInfo>>()
    val notifications: LiveData<List<NotificationInfo>> = _notifications
    
    private val _currentTime = MutableLiveData<String>()
    val currentTime: LiveData<String> = _currentTime
    
    private val _currentDate = MutableLiveData<String>()
    val currentDate: LiveData<String> = _currentDate
    
    private val _batteryLevel = MutableLiveData<Int>()
    val batteryLevel: LiveData<Int> = _batteryLevel
    
    private val _isWifiConnected = MutableLiveData<Boolean>()
    val isWifiConnected: LiveData<Boolean> = _isWifiConnected
    
    private val _isBluetoothConnected = MutableLiveData<Boolean>()
    val isBluetoothConnected: LiveData<Boolean> = _isBluetoothConnected
    
    private val _isDrawerOpen = MutableLiveData(false)
    val isDrawerOpen: LiveData<Boolean> = _isDrawerOpen
    
    private val _isNotificationPanelOpen = MutableLiveData(false)
    val isNotificationPanelOpen: LiveData<Boolean> = _isNotificationPanelOpen

    init {
        loadApps()
        updateTime()
        startClockUpdater()
    }

    fun loadApps() {
        viewModelScope.launch {
            val allApps = appRepository.getInstalledApps()
            _apps.value = allApps
            _favoriteApps.value = appRepository.getFavoriteApps()
        }
    }

    fun launchApp(appInfo: AppInfo) {
        appRepository.launchApp(appInfo.packageName)
    }

    fun toggleFavorite(packageName: String) {
        if (appRepository.isFavorite(packageName)) {
            appRepository.removeFavoriteApp(packageName)
        } else {
            appRepository.addFavoriteApp(packageName)
        }
        loadApps()
    }

    fun isFavorite(packageName: String): Boolean {
        return appRepository.isFavorite(packageName)
    }

    fun sortAppsByName() {
        _apps.value = _apps.value?.let { appRepository.sortAppsByName(it) }
    }

    fun sortAppsByUsage() {
        _apps.value = _apps.value?.let { appRepository.sortAppsByUsage(it) }
    }

    fun toggleDrawer() {
        _isDrawerOpen.value = !(_isDrawerOpen.value ?: false)
    }

    fun closeDrawer() {
        _isDrawerOpen.value = false
    }

    fun toggleNotificationPanel() {
        _isNotificationPanelOpen.value = !(_isNotificationPanelOpen.value ?: false)
    }

    fun closeNotificationPanel() {
        _isNotificationPanelOpen.value = false
    }

    private fun updateTime() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val minute = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
        _currentTime.value = "$hour:$minute"
        
        val weekDays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
        val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val weekDay = weekDays[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        _currentDate.value = "${calendar.get(Calendar.YEAR)}年$month月$day日 $weekDay"
    }

    private fun startClockUpdater() {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                updateTime()
            }
        }, 0, 1000)
    }

    fun updateBatteryLevel(context: Context) {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        intent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level > 0 && scale > 0) {
                _batteryLevel.value = (level * 100 / scale)
            }
        }
    }

    fun updateWifiStatus(context: Context) {
        _isWifiConnected.value = true
    }

    fun updateBluetoothStatus(context: Context) {
        _isBluetoothConnected.value = true
    }
}
