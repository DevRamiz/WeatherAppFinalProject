package com.example.finalproject

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var darkModeSwitch: Switch
    private lateinit var unitSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("WeatherAppPrefs", MODE_PRIVATE)

        darkModeSwitch = findViewById(R.id.darkModeSwitch)
        unitSwitch = findViewById(R.id.unitSwitch)

        val isDarkMode = prefs.getBoolean("DarkMode", false)
        val isFahrenheit = prefs.getBoolean("UseFahrenheit", false)

        darkModeSwitch.isChecked = isDarkMode
        unitSwitch.isChecked = isFahrenheit

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("DarkMode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            recreate()
            Toast.makeText(this, "Dark mode ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        unitSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("UseFahrenheit", isChecked).apply()
            Toast.makeText(this, "Unit changed to ${if (isChecked) "Fahrenheit" else "Celsius"}", Toast.LENGTH_SHORT).show()
        }
    }
}
