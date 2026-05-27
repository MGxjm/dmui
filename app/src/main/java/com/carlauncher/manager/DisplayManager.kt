package com.carlauncher.manager

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.carlauncher.data.model.DisplaySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DisplayManager(private val context: Context) {

    companion object {
        private const val TAG = "DisplayManager"
    }

    private val adbManager = AdbManager(context)

    private suspend fun executeDisplayCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Executing display command: $command")
            val result = adbManager.executeAdbCommand(command)
            val success = !result.contains("Error") && !result.contains("Exception")
            if (!success) {
                Log.e(TAG, "Command failed: $result")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute display command", e)
            false
        }
    }

    suspend fun getCurrentScreenBrightness(): Int {
        return try {
            val brightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            (brightness * 100 / 255).coerceIn(0, 100)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get screen brightness", e)
            80
        }
    }

    suspend fun turnOffScreen(): Boolean {
        return executeDisplayCommand("input keyevent 26")
    }

    suspend fun turnOnScreen(): Boolean {
        return executeDisplayCommand("input keyevent 26")
    }

    suspend fun setScreenBrightness(brightness: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val brightnessValue = (brightness * 255 / 100).coerceIn(0, 255)
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightnessValue)
                Log.d(TAG, "Screen brightness set to $brightness% via system API")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set screen brightness via API, trying ADB", e)
                executeDisplayCommand("settings put system screen_brightness ${(brightness * 255 / 100).coerceIn(0, 255)}")
            }
        }
    }

    suspend fun setInstrumentBrightness(brightness: Int): Boolean {
        return executeDisplayCommand("settings put system instrument_brightness ${(brightness * 255 / 100).coerceIn(0, 255)}")
    }

    suspend fun projectMapToInstrument(): Boolean {
        return executeDisplayCommand("am start -n com.autonavi.amapauto/.MainActivity --ez projection true")
    }

    suspend fun stopMapProjection(): Boolean {
        return executeDisplayCommand("am broadcast -a com.carlauncher.STOP_PROJECTION")
    }

    suspend fun startFloatingMap(packageName: String): Boolean {
        return executeDisplayCommand("am start -n $packageName/.FloatingMapService --ez floating true")
    }

    suspend fun stopFloatingMap(): Boolean {
        return executeDisplayCommand("am broadcast -a com.carlauncher.STOP_FLOATING_MAP")
    }

    suspend fun getDisplaySettings(): DisplaySettings {
        return DisplaySettings(
            screenBrightness = getCurrentScreenBrightness(),
            instrumentBrightness = 60,
            isScreenOn = true,
            isMapProjected = false
        )
    }

    suspend fun setDisplaySettings(settings: DisplaySettings): Boolean {
        var success = true
        if (!setScreenBrightness(settings.screenBrightness)) success = false
        if (!setInstrumentBrightness(settings.instrumentBrightness)) success = false
        return success
    }
}