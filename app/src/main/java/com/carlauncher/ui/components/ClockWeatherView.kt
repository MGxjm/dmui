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

class ClockWeatherView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var weatherIcon: ImageView
    private lateinit var temperatureText: TextView
    private lateinit var weatherDescText: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_clock_weather, this, true)
        initViews()
    }

    private fun initViews() {
        timeText = findViewById(R.id.clock_time)
        dateText = findViewById(R.id.clock_date)
        weatherIcon = findViewById(R.id.weather_icon)
        temperatureText = findViewById(R.id.weather_temp)
        weatherDescText = findViewById(R.id.weather_desc)
    }

    fun bindTime(time: LiveData<String>, owner: LifecycleOwner) {
        time.observe(owner) { timeText.text = it }
    }

    fun bindDate(date: LiveData<String>, owner: LifecycleOwner) {
        date.observe(owner) { dateText.text = it }
    }

    fun setWeather(temperature: String, description: String) {
        temperatureText.text = temperature
        weatherDescText.text = description
        weatherIcon.setImageResource(R.drawable.ic_weather_sunny)
    }
}
