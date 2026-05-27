package com.carlauncher.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.*
import com.carlauncher.R
import com.carlauncher.manager.DisplayManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DisplayControlPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    
    private val displayManager = DisplayManager(context)
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private lateinit var screenOffButton: MaterialButton
    private lateinit var screenOnButton: MaterialButton
    private lateinit var screenBrightnessSlider: Slider
    private lateinit var screenBrightnessText: TextView
    private lateinit var instrumentBrightnessSlider: Slider
    private lateinit var instrumentBrightnessText: TextView
    private lateinit var projectMapButton: MaterialButton
    private lateinit var stopMapButton: MaterialButton
    
    init {
        LayoutInflater.from(context).inflate(R.layout.view_display_control_panel, this, true)
        initViews()
        setupClickListeners()
    }
    
    private fun initViews() {
        screenOffButton = findViewById(R.id.screen_off_button)
        screenOnButton = findViewById(R.id.screen_on_button)
        screenBrightnessSlider = findViewById(R.id.screen_brightness_seekbar)
        screenBrightnessText = findViewById(R.id.screen_brightness_text)
        instrumentBrightnessSlider = findViewById(R.id.instrument_brightness_seekbar)
        instrumentBrightnessText = findViewById(R.id.instrument_brightness_text)
        projectMapButton = findViewById(R.id.project_map_button)
        stopMapButton = findViewById(R.id.stop_map_button)
        
        // 初始化亮度值
        scope.launch {
            try {
                val currentBrightness = displayManager.getCurrentScreenBrightness()
                screenBrightnessSlider.value = currentBrightness.toFloat()
                screenBrightnessText.text = "$currentBrightness%"
            } catch (e: Exception) {
                screenBrightnessSlider.value = 80f
                screenBrightnessText.text = "80%"
            }
        }
    }
    
    private fun setupClickListeners() {
        screenOffButton.setOnClickListener {
            scope.launch { displayManager.turnOffScreen() }
        }
        
        screenOnButton.setOnClickListener {
            scope.launch { displayManager.turnOnScreen() }
        }
        
        screenBrightnessSlider.addOnChangeListener { slider, value, fromUser ->
            screenBrightnessText.text = "${value.toInt()}%"
            if (fromUser) {
                scope.launch { displayManager.setScreenBrightness(value.toInt()) }
            }
        }
        
        instrumentBrightnessSlider.addOnChangeListener { slider, value, fromUser ->
            instrumentBrightnessText.text = "${value.toInt()}%"
            if (fromUser) {
                scope.launch { displayManager.setInstrumentBrightness(value.toInt()) }
            }
        }
        
        projectMapButton.setOnClickListener {
            scope.launch { displayManager.projectMapToInstrument() }
        }
        
        stopMapButton.setOnClickListener {
            scope.launch { displayManager.stopMapProjection() }
        }
    }
}