package com.carlauncher.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.carlauncher.R
import com.carlauncher.utils.setVisibility

class StatusBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var batteryIcon: ImageView
    private lateinit var batteryText: TextView
    private lateinit var wifiIcon: ImageView
    private lateinit var bluetoothIcon: ImageView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_status_bar, this, true)
        initViews()
    }

    private fun initViews() {
        timeText = findViewById(R.id.status_bar_time)
        dateText = findViewById(R.id.status_bar_date)
        batteryIcon = findViewById(R.id.status_bar_battery_icon)
        batteryText = findViewById(R.id.status_bar_battery_text)
        wifiIcon = findViewById(R.id.status_bar_wifi_icon)
        bluetoothIcon = findViewById(R.id.status_bar_bluetooth_icon)
    }

    fun bindTime(time: LiveData<String>, owner: LifecycleOwner) {
        time.observe(owner) { timeText.text = it }
    }

    fun bindDate(date: LiveData<String>, owner: LifecycleOwner) {
        date.observe(owner) { dateText.text = it }
    }

    fun bindBatteryLevel(level: LiveData<Int>, owner: LifecycleOwner) {
        level.observe(owner) {
            batteryText.text = "$it%"
            batteryIcon.setImageResource(
                when {
                    it >= 80 -> R.drawable.ic_battery_full
                    it >= 50 -> R.drawable.ic_battery_high
                    it >= 20 -> R.drawable.ic_battery_medium
                    else -> R.drawable.ic_battery_low
                }
            )
        }
    }

    fun bindWifiConnected(isConnected: LiveData<Boolean>, owner: LifecycleOwner) {
        isConnected.observe(owner) {
            wifiIcon.setVisibility(it)
        }
    }

    fun bindBluetoothConnected(isConnected: LiveData<Boolean>, owner: LifecycleOwner) {
        isConnected.observe(owner) {
            bluetoothIcon.setVisibility(it)
        }
    }
}
