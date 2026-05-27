package com.carlauncher.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.carlauncher.data.model.Theme
import com.carlauncher.data.model.UserSettings
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("car_launcher_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun saveSettings(settings: UserSettings) {
        val jsonString = json.encodeToString(settings)
        prefs.edit().putString("user_settings", jsonString).apply()
    }

    fun getSettings(): UserSettings {
        val jsonString = prefs.getString("user_settings", null)
        return jsonString?.let {
            try {
                json.decodeFromString(it)
            } catch (e: Exception) {
                UserSettings()
            }
        } ?: UserSettings()
    }

    fun saveFavoriteApps(apps: List<String>) {
        prefs.edit().putString("favorite_apps", json.encodeToString(apps)).apply()
    }

    fun getFavoriteApps(): List<String> {
        val jsonString = prefs.getString("favorite_apps", null)
        return jsonString?.let {
            try {
                json.decodeFromString(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    fun saveTheme(theme: Theme) {
        prefs.edit().putString("theme", theme.name).apply()
    }

    fun getTheme(): Theme {
        val themeString = prefs.getString("theme", Theme.DARK.name)
        return try {
            Theme.valueOf(themeString ?: Theme.DARK.name)
        } catch (e: Exception) {
            Theme.DARK
        }
    }

    fun incrementAppLaunchCount(packageName: String) {
        val count = getAppLaunchCount(packageName) + 1
        prefs.edit().putInt("launch_count_$packageName", count).apply()
        prefs.edit().putLong("last_used_$packageName", System.currentTimeMillis()).apply()
    }

    fun getAppLaunchCount(packageName: String): Int {
        return prefs.getInt("launch_count_$packageName", 0)
    }

    fun getAppLastUsed(packageName: String): Long {
        return prefs.getLong("last_used_$packageName", 0)
    }
}
