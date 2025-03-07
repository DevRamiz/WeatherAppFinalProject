package com.example.finalproject

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val API_KEY = "afca018e7fed05b006990a0f204961fc"
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var cityNameText: TextView
    private lateinit var temperatureText: TextView
    private lateinit var feelsLikeText: TextView
    private lateinit var minTempText: TextView
    private lateinit var maxTempText: TextView
    private lateinit var humidityText: TextView
    private lateinit var windText: TextView
    private lateinit var pressureText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var weatherIcon: ImageView
    private lateinit var fetchButton: Button
    private lateinit var cityInput: EditText
    private lateinit var btnSettings: Button
    private lateinit var btnWeatherHistory: Button
    private lateinit var btnForecast: Button
    private lateinit var btnFavorites: Button
    private var isFahrenheit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        isFahrenheit = prefs.getBoolean("UseFahrenheit", false)

        databaseHelper = DatabaseHelper(this)

        cityNameText = findViewById(R.id.cityNameText)
        temperatureText = findViewById(R.id.temperatureText)
        feelsLikeText = findViewById(R.id.feelsLikeText)
        minTempText = findViewById(R.id.minTempText)
        maxTempText = findViewById(R.id.maxTempText)
        humidityText = findViewById(R.id.humidityText)
        windText = findViewById(R.id.windText)
        pressureText = findViewById(R.id.pressureText)
        descriptionText = findViewById(R.id.descriptionText)
        weatherIcon = findViewById(R.id.weatherIcon)
        fetchButton = findViewById(R.id.fetchWeatherButton)
        cityInput = findViewById(R.id.cityNameInput)
        btnSettings = findViewById(R.id.btnSettings)
        btnWeatherHistory = findViewById(R.id.btnWeatherHistory)
        btnForecast = findViewById(R.id.btnForecast)
        btnFavorites = findViewById(R.id.btnFavorites)

        fetchButton.setOnClickListener {
            val cityName = cityInput.text.toString()
            if (cityName.isNotEmpty()) {
                fetchWeatherData(cityName)
            } else {
                cityInput.error = "Enter a city name"
            }
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnWeatherHistory.setOnClickListener {
            startActivity(Intent(this, WeatherHistoryActivity::class.java))
        }

        btnForecast.setOnClickListener {
            startActivity(Intent(this, ForecastActivity::class.java))
        }

        btnFavorites.setOnClickListener {
            startActivity(Intent(this, FavoriteLocationsActivity::class.java))
        }

        intent.getStringExtra("city_name")?.let {
            cityInput.setText(it)
            fetchWeatherData(it)
        }
    }

    private fun fetchWeatherData(city: String) {
        val prefs = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        val isFahrenheit = prefs.getBoolean("UseFahrenheit", false)
        val units = if (isFahrenheit) "imperial" else "metric"

        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$API_KEY&units=$units"
        Log.d("WeatherApp", "Fetching weather from: $url")

        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            try {
                val response = client.newCall(request).execute()
                val result = response.body?.string()

                Log.d("WeatherApp", "API Response: $result")

                if (!response.isSuccessful) {
                    Log.e("WeatherApp", "API request failed with response code: ${response.code}")
                    return@execute
                }

                runOnUiThread { updateUI(result) }
            } catch (e: IOException) {
                Log.e("WeatherApp", "API request failed: ${e.message}", e)
            }
        }
    }

    private fun updateUI(result: String?) {
        if (result == null) {
            Log.e("WeatherApp", "Received null response")
            return
        }

        try {
            val jsonObject = JSONObject(result)

            val weather = jsonObject.getJSONArray("weather").getJSONObject(0)
            val icon = weather.getString("icon")
            val description = weather.getString("description")
            val temp = jsonObject.getJSONObject("main").getDouble("temp")
            val feelsLike = jsonObject.getJSONObject("main").getDouble("feels_like")
            val minTemp = jsonObject.getJSONObject("main").getDouble("temp_min")
            val maxTemp = jsonObject.getJSONObject("main").getDouble("temp_max")
            val humidity = jsonObject.getJSONObject("main").getInt("humidity")
            val windSpeed = jsonObject.getJSONObject("wind").getDouble("speed")
            val pressure = jsonObject.getJSONObject("main").getInt("pressure")
            val cityName = jsonObject.getString("name")

            val displayTemp = convertTemperature(temp)
            val displayFeelsLike = convertTemperature(feelsLike)
            val displayMinTemp = convertTemperature(minTemp)
            val displayMaxTemp = convertTemperature(maxTemp)

            cityNameText.text = cityName
            temperatureText.text = "Temperature: $displayTemp"
            feelsLikeText.text = "Feels Like: $displayFeelsLike"
            minTempText.text = "Min Temp: $displayMinTemp"
            maxTempText.text = "Max Temp: $displayMaxTemp"
            humidityText.text = "Humidity: $humidity%"
            windText.text = "Wind: $windSpeed ${if (isFahrenheit) "mph" else "m/s"}"
            pressureText.text = "Pressure: $pressure hPa"

            Glide.with(this)
                .load("https://openweathermap.org/img/wn/$icon@2x.png")
                .into(weatherIcon)

            saveWeatherToHistory(cityName, temp, feelsLike, minTemp, maxTemp, humidity, windSpeed, pressure, description)

            Log.d("WeatherApp", "Weather updated successfully for $cityName")

        } catch (e: JSONException) {
            Log.e("WeatherApp", "JSON Parsing Error", e)
        }
    }

    private fun saveWeatherToHistory(
        city: String, temp: Double, feelsLike: Double, minTemp: Double,
        maxTemp: Double, humidity: Int, windSpeed: Double, pressure: Int, description: String
    ) {
        val prefs = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        val isFahrenheit = prefs.getBoolean("UseFahrenheit", false)
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        databaseHelper.addWeatherRecord(
            city, date, temp, feelsLike, minTemp, maxTemp, humidity, windSpeed, pressure, description, isFahrenheit
        )

        Log.d("WeatherApp", "Weather saved to history for $city at $date with unit ${if (isFahrenheit) "°F" else "°C"}")
    }

    private fun convertTemperature(temp: Double): String {
        val prefs = getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)
        val isFahrenheit = prefs.getBoolean("UseFahrenheit", false)

        return if (isFahrenheit) {
            String.format("%.1f°F", temp)
        } else {
            String.format("%.1f°C", temp)
        }
    }
}
