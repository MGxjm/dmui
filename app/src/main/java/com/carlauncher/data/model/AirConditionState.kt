package com.carlauncher.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AirConditionState(
    val temperature: Float = 24.0f,
    val windLevel: Int = 3,
    val mode: AirMode = AirMode.AUTO,
    val isOn: Boolean = true
) : Parcelable

@Parcelize
enum class AirMode : Parcelable {
    AUTO, COOL, HEAT, VENT, DEFROST
}
