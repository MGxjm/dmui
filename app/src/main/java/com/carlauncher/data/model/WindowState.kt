package com.carlauncher.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WindowState(
    val frontLeft: Boolean = false,
    val frontRight: Boolean = false,
    val rearLeft: Boolean = false,
    val rearRight: Boolean = false
) : Parcelable

enum class WindowPosition {
    FRONT_LEFT, FRONT_RIGHT, REAR_LEFT, REAR_RIGHT
}
