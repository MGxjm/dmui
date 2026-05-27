package com.carlauncher.data.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val lastUsed: Long,
    val launchCount: Int
) : Comparable<AppInfo> {
    override fun compareTo(other: AppInfo): Int {
        return appName.compareTo(other.appName, ignoreCase = true)
    }
}
