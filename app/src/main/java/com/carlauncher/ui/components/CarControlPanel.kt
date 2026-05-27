package com.carlauncher.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.*
import com.carlauncher.R
import com.carlauncher.data.model.AirMode
import com.carlauncher.data.model.WindowPosition
import com.carlauncher.manager.CarServiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CarControlPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    
    private val carServiceManager = CarServiceManager(context)
    private val scope = CoroutineScope(Dispatchers.Main)
    
    // 空调控制
    private lateinit var acPowerButton: Button
    private lateinit var tempUpButton: Button
    private lateinit var tempDownButton: Button
    private lateinit var tempText: TextView
    private lateinit var windUpButton: Button
    private lateinit var windDownButton: Button
    private lateinit var windText: TextView
    private lateinit var modeAutoButton: Button
    private lateinit var modeCoolButton: Button
    private lateinit var modeHeatButton: Button
    
    // 车窗控制
    private lateinit var windowFlButton: Button
    private lateinit var windowFrButton: Button
    private lateinit var windowRlButton: Button
    private lateinit var windowRrButton: Button
    private lateinit var windowAllUpButton: Button
    private lateinit var windowAllDownButton: Button
    
    // 天窗控制
    private lateinit var sunroofOpenButton: Button
    private lateinit var sunroofCloseButton: Button
    private lateinit var sunshadeOpenButton: Button
    private lateinit var sunshadeCloseButton: Button
    
    // 后备箱控制
    private lateinit var trunkOpenButton: Button
    private lateinit var trunkCloseButton: Button
    
    private var currentTemp = 24.0f
    private var currentWindLevel = 3
    private var isAcOn = true
    
    init {
        LayoutInflater.from(context).inflate(R.layout.view_car_control_panel, this, true)
        initViews()
        setupClickListeners()
    }
    
    private fun initViews() {
        // 空调控制
        acPowerButton = findViewById(R.id.ac_power_button)
        tempUpButton = findViewById(R.id.temp_up_button)
        tempDownButton = findViewById(R.id.temp_down_button)
        tempText = findViewById(R.id.temp_text)
        windUpButton = findViewById(R.id.wind_up_button)
        windDownButton = findViewById(R.id.wind_down_button)
        windText = findViewById(R.id.wind_text)
        modeAutoButton = findViewById(R.id.mode_auto_button)
        modeCoolButton = findViewById(R.id.mode_cool_button)
        modeHeatButton = findViewById(R.id.mode_heat_button)
        
        // 车窗控制
        windowFlButton = findViewById(R.id.window_fl_button)
        windowFrButton = findViewById(R.id.window_fr_button)
        windowRlButton = findViewById(R.id.window_rl_button)
        windowRrButton = findViewById(R.id.window_rr_button)
        windowAllUpButton = findViewById(R.id.window_all_up_button)
        windowAllDownButton = findViewById(R.id.window_all_down_button)
        
        // 天窗控制
        sunroofOpenButton = findViewById(R.id.sunroof_open_button)
        sunroofCloseButton = findViewById(R.id.sunroof_close_button)
        sunshadeOpenButton = findViewById(R.id.sunshade_open_button)
        sunshadeCloseButton = findViewById(R.id.sunshade_close_button)
        
        // 后备箱控制
        trunkOpenButton = findViewById(R.id.trunk_open_button)
        trunkCloseButton = findViewById(R.id.trunk_close_button)
    }
    
    private fun setupClickListeners() {
        // 空调控制
        acPowerButton.setOnClickListener {
            isAcOn = !isAcOn
            scope.launch {
                if (isAcOn) {
                    carServiceManager.turnOnAirCondition()
                } else {
                    carServiceManager.turnOffAirCondition()
                }
            }
        }
        
        tempUpButton.setOnClickListener {
            if (currentTemp < 30.0f) {
                currentTemp += 0.5f
                updateTempDisplay()
                scope.launch {
                    carServiceManager.setAirConditionTemp(currentTemp)
                }
            }
        }
        
        tempDownButton.setOnClickListener {
            if (currentTemp > 16.0f) {
                currentTemp -= 0.5f
                updateTempDisplay()
                scope.launch {
                    carServiceManager.setAirConditionTemp(currentTemp)
                }
            }
        }
        
        windUpButton.setOnClickListener {
            if (currentWindLevel < 7) {
                currentWindLevel++
                updateWindDisplay()
                scope.launch {
                    carServiceManager.setAirConditionWindLevel(currentWindLevel)
                }
            }
        }
        
        windDownButton.setOnClickListener {
            if (currentWindLevel > 1) {
                currentWindLevel--
                updateWindDisplay()
                scope.launch {
                    carServiceManager.setAirConditionWindLevel(currentWindLevel)
                }
            }
        }
        
        modeAutoButton.setOnClickListener {
            scope.launch { carServiceManager.setAirConditionMode(AirMode.AUTO) }
        }
        
        modeCoolButton.setOnClickListener {
            scope.launch { carServiceManager.setAirConditionMode(AirMode.COOL) }
        }
        
        modeHeatButton.setOnClickListener {
            scope.launch { carServiceManager.setAirConditionMode(AirMode.HEAT) }
        }
        
        // 车窗控制
        windowFlButton.setOnClickListener {
            scope.launch { carServiceManager.controlWindow(WindowPosition.FRONT_LEFT, true) }
        }
        
        windowFrButton.setOnClickListener {
            scope.launch { carServiceManager.controlWindow(WindowPosition.FRONT_RIGHT, true) }
        }
        
        windowRlButton.setOnClickListener {
            scope.launch { carServiceManager.controlWindow(WindowPosition.REAR_LEFT, true) }
        }
        
        windowRrButton.setOnClickListener {
            scope.launch { carServiceManager.controlWindow(WindowPosition.REAR_RIGHT, true) }
        }
        
        windowAllUpButton.setOnClickListener {
            scope.launch { carServiceManager.controlAllWindows(false) }
        }
        
        windowAllDownButton.setOnClickListener {
            scope.launch { carServiceManager.controlAllWindows(true) }
        }
        
        // 天窗控制
        sunroofOpenButton.setOnClickListener {
            scope.launch { carServiceManager.openSunroof() }
        }
        
        sunroofCloseButton.setOnClickListener {
            scope.launch { carServiceManager.closeSunroof() }
        }
        
        sunshadeOpenButton.setOnClickListener {
            scope.launch { carServiceManager.controlSunshade(true) }
        }
        
        sunshadeCloseButton.setOnClickListener {
            scope.launch { carServiceManager.controlSunshade(false) }
        }
        
        // 后备箱控制
        trunkOpenButton.setOnClickListener {
            scope.launch { carServiceManager.openTrunk() }
        }
        
        trunkCloseButton.setOnClickListener {
            scope.launch { carServiceManager.closeTrunk() }
        }
    }
    
    private fun updateTempDisplay() {
        tempText.text = "${currentTemp}°C"
    }
    
    private fun updateWindDisplay() {
        windText.text = "风力: $currentWindLevel"
    }
}
