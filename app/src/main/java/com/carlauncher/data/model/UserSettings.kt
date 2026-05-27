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
    val favoriteApps: List<String> = emptyList()
) : Parcelable
