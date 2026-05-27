package com.carlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.carlauncher.data.model.AppInfo
import com.carlauncher.data.prefs.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {
    private val packageManager = context.packageManager
    private val prefs = PreferencesManager(context)

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
        val apps = mutableListOf<AppInfo>()

        for (resolveInfo in resolveInfoList) {
            try {
                val packageName = resolveInfo.activityInfo.packageName
                val appName = resolveInfo.loadLabel(packageManager).toString()
                val icon = resolveInfo.loadIcon(packageManager)
                val lastUsed = prefs.getAppLastUsed(packageName)
                val launchCount = prefs.getAppLaunchCount(packageName)

                apps.add(
                    AppInfo(
                        packageName = packageName,
                        appName = appName,
                        icon = icon,
                        lastUsed = lastUsed,
                        launchCount = launchCount
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        apps
    }

    suspend fun getFavoriteApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val favoritePackageNames = prefs.getFavoriteApps()
        val allApps = getInstalledApps()
        allApps.filter { favoritePackageNames.contains(it.packageName) }
    }

    fun launchApp(packageName: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            launchIntent?.let {
                prefs.incrementAppLaunchCount(packageName)
                context.startActivity(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addFavoriteApp(packageName: String) {
        val favorites = prefs.getFavoriteApps().toMutableList()
        if (!favorites.contains(packageName)) {
            favorites.add(packageName)
            prefs.saveFavoriteApps(favorites)
        }
    }

    fun removeFavoriteApp(packageName: String) {
        val favorites = prefs.getFavoriteApps().toMutableList()
        favorites.remove(packageName)
        prefs.saveFavoriteApps(favorites)
    }

    fun isFavorite(packageName: String): Boolean {
        return prefs.getFavoriteApps().contains(packageName)
    }

    fun sortAppsByUsage(apps: List<AppInfo>): List<AppInfo> {
        return apps.sortedByDescending { it.launchCount }
    }

    fun sortAppsByName(apps: List<AppInfo>): List<AppInfo> {
        return apps.sorted()
    }
}
