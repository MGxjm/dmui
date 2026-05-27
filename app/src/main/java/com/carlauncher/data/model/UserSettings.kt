package com.carlauncher.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Theme : Parcelable {
    LIGHT, DARK, AUTO
}

@Parcelize
data class UserSettings(
    val theme: Theme = Theme.DARK,
    val wallpaperUri: String? = null,
    val favoriteApps: List<String> = emptyList(),
    val adbEnabled: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val floatingWindowEnabled: Boolean = false
) : Parcelable
