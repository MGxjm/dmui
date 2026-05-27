package com.carlauncher.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DisplaySettings(
    val screenBrightness: Int = 80,
    val instrumentBrightness: Int = 60,
    val isScreenOn: Boolean = true,
    val isMapProjected: Boolean = false
) : Parcelable
