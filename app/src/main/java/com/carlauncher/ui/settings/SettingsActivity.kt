package com.carlauncher.ui.settings

import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.carlauncher.R
import com.carlauncher.data.model.Theme
import com.carlauncher.data.prefs.PreferencesManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var themeRadioGroup: RadioGroup
    private lateinit var saveButton: Button
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        prefs = PreferencesManager(this)
        initViews()
        setupClickListeners()
        loadCurrentSettings()
    }

    private fun initViews() {
        themeRadioGroup = findViewById(R.id.theme_radio_group)
        saveButton = findViewById(R.id.settings_save)
    }

    private fun setupClickListeners() {
        saveButton.setOnClickListener { saveSettings() }
    }

    private fun loadCurrentSettings() {
        val currentTheme = prefs.getTheme()
        when (currentTheme) {
            Theme.LIGHT -> themeRadioGroup.check(R.id.theme_light)
            Theme.DARK -> themeRadioGroup.check(R.id.theme_dark)
            Theme.AUTO -> themeRadioGroup.check(R.id.theme_auto)
        }
    }

    private fun saveSettings() {
        val selectedTheme = when (themeRadioGroup.checkedRadioButtonId) {
            R.id.theme_light -> Theme.LIGHT
            R.id.theme_dark -> Theme.DARK
            R.id.theme_auto -> Theme.AUTO
            else -> Theme.DARK
        }
        prefs.saveTheme(selectedTheme)
        finish()
    }
}
