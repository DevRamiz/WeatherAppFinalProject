package com.example.finalproject

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Executors

class ForecastActivity : AppCompatActivity() {
    private val API_KEY = "afca018e7fed05b006990a0f204961fc"
    private lateinit var forecastListView: ListView
    private lateinit var hourlyForecastView: ListView
    private lateinit var cityInput: EditText
    private lateinit var fetchButton: Button
    private var isFahrenheit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forecast)

        forecastListView = findViewById(R.id.forecastListView)
        hourlyForecastView = findViewById(R.id.hourlyForecastView)
        cityInput = findViewById(R.id.cityInput)
        fetchButton = findViewById(R.id.fetchForecastButton)

        val prefs = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        isFahrenheit = prefs.getBoolean("UseFahrenheit", false)

        val cityFromFavorites = intent.getStringExtra("city_name")
        if (!cityFromFavorites.isNullOrEmpty()) {
            cityInput.setText(cityFromFavorites)
            fetchWeatherForecast(cityFromFavorites)
        }

        fetchButton.setOnClickListener {
            val cityName = cityInput.text.toString()
            if (cityName.isNotEmpty()) {
                fetchWeatherForecast(cityName)
            } else {
                cityInput.error = "Enter a city name"
            }
        }
    }

    private fun fetchWeatherForecast(city: String) {
        val prefs = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        isFahrenheit = prefs.getBoolean("UseFahrenheit", false)
        val units = if (isFahrenheit) "imperial" else "metric"

        val url = "https://api.openweathermap.org/data/2.5/forecast?q=$city&appid=$API_KEY&units=$units"
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            try {
                val response = client.newCall(request).execute()
                val result = response.body?.string()
                runOnUiThread { updateUI(result) }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Failed to load forecast. Check connection.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUI(result: String?) {
        result?.let {
            val jsonObject = JSONObject(it)
            val forecastArray = jsonObject.getJSONArray("list")

            val dailyForecast = ArrayList<String>()
            val hourlyForecast = ArrayList<String>()

            val unitSymbol = if (isFahrenheit) "°F" else "°C"

            for (i in 0 until forecastArray.length() step 8) {
                val entry = forecastArray.getJSONObject(i)
                val temp = entry.getJSONObject("main").getDouble("temp")
                val date = entry.getString("dt_txt").split(" ")[0]
                dailyForecast.add("$date - Temp: ${String.format("%.1f", temp)}$unitSymbol")
            }

            for (i in 0 until 8) {
                val entry = forecastArray.getJSONObject(i)
                val temp = entry.getJSONObject("main").getDouble("temp")
                val time = entry.getString("dt_txt").split(" ")[1]
                hourlyForecast.add("$time - Temp: ${String.format("%.1f", temp)}$unitSymbol")
            }

            forecastListView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, dailyForecast)
            hourlyForecastView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, hourlyForecast)
        }
    }
}
