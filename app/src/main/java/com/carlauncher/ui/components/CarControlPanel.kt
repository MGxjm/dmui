package com.carlauncher.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.*
import com.carlauncher.R
import com.carlauncher.manager.CarServiceManager
import com.google.android.material.button.MaterialButton
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
    
    private lateinit var acPowerButton: MaterialButton
    private lateinit var tempUpButton: MaterialButton
    private lateinit var tempDownButton: MaterialButton
    private lateinit var fanUpButton: MaterialButton
    private lateinit var fanDownButton: MaterialButton
    private lateinit var acModeButton: MaterialButton
    
    private lateinit var windowFrontUp: MaterialButton
    private lateinit var windowFrontDown: MaterialButton
    private lateinit var windowRearUp: MaterialButton
    private lateinit var windowRearDown: MaterialButton
    
    private lateinit var sunroofOpen: MaterialButton
    private lateinit var sunroofClose: MaterialButton
    private lateinit var trunkOpen: MaterialButton
    
    init {
        LayoutInflater.from(context).inflate(R.layout.view_car_control_panel, this, true)
        initViews()
        setupClickListeners()
    }
    
    private fun initViews() {
        acPowerButton = findViewById(R.id.ac_power_button)
        tempUpButton = findViewById(R.id.temp_up_button)
        tempDownButton = findViewById(R.id.temp_down_button)
        fanUpButton = findViewById(R.id.fan_up_button)
        fanDownButton = findViewById(R.id.fan_down_button)
        acModeButton = findViewById(R.id.ac_mode_button)
        
        windowFrontUp = findViewById(R.id.window_front_up)
        windowFrontDown = findViewById(R.id.window_front_down)
        windowRearUp = findViewById(R.id.window_rear_up)
        windowRearDown = findViewById(R.id.window_rear_down)
        
        sunroofOpen = findViewById(R.id.sunroof_open)
        sunroofClose = findViewById(R.id.sunroof_close)
        trunkOpen = findViewById(R.id.trunk_open)
    }
    
    private fun setupClickListeners() {
        acPowerButton.setOnClickListener {
            scope.launch { carServiceManager.turnOnAirCondition() }
        }
        
        tempUpButton.setOnClickListener {
            scope.launch { carServiceManager.setAirConditionTemp(25.0f) }
        }
        
        tempDownButton.setOnClickListener {
            scope.launch { carServiceManager.setAirConditionTemp(23.0f) }
        }
        
        fanUpButton.setOnClickListener {
            scope.launch { carServiceManager.setAirConditionWindLevel(4) }
        }
        
        fanDownButton.setOnClickListener {
            scope.launch { carServiceManager.setAirConditionWindLevel(2) }
        }
        
        acModeButton.setOnClickListener {
            scope.launch { carServiceManager.setAirConditionMode(com.carlauncher.data.model.AirMode.AUTO) }
        }
        
        windowFrontUp.setOnClickListener {
            scope.launch { carServiceManager.controlWindow(com.carlauncher.data.model.WindowPosition.FRONT_LEFT, false) }
        }
        
        windowFrontDown.setOnClickListener {
            scope.launch { carServiceManager.controlWindow(com.carlauncher.data.model.WindowPosition.FRONT_LEFT, true) }
        }
        
        windowRearUp.setOnClickListener {
            scope.launch { carServiceManager.controlWindow(com.carlauncher.data.model.WindowPosition.REAR_LEFT, false) }
        }
        
        windowRearDown.setOnClickListener {
            scope.launch { carServiceManager.controlWindow(com.carlauncher.data.model.WindowPosition.REAR_LEFT, true) }
        }
        
        sunroofOpen.setOnClickListener {
            scope.launch { carServiceManager.openSunroof() }
        }
        
        sunroofClose.setOnClickListener {
            scope.launch { carServiceManager.closeSunroof() }
        }
        
        trunkOpen.setOnClickListener {
            scope.launch { carServiceManager.openTrunk() }
        }
    }
}
