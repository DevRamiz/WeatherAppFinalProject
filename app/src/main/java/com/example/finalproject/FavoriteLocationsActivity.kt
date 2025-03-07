package com.example.finalproject

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class FavoriteLocationsActivity : AppCompatActivity() {
    private lateinit var cityInput: EditText
    private lateinit var btnAddCity: Button
    private lateinit var btnClearFavorites: Button
    private lateinit var favoriteCitiesList: ListView
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var adapter: ArrayAdapter<String>
    private var favoriteCities = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_locations)

        cityInput = findViewById(R.id.cityInput)
        btnAddCity = findViewById(R.id.btnAddCity)
        btnClearFavorites = findViewById(R.id.btnClearFavorites)
        favoriteCitiesList = findViewById(R.id.favoriteCitiesList)

        databaseHelper = DatabaseHelper(this)

        loadFavoriteCities()

        btnAddCity.setOnClickListener {
            val cityName = cityInput.text.toString().trim()
            if (cityName.isNotEmpty()) {
                addFavoriteCity(cityName)
            } else {
                Toast.makeText(this, "Enter a city name", Toast.LENGTH_SHORT).show()
            }
        }

        btnClearFavorites.setOnClickListener {
            clearAllFavorites()
        }

        favoriteCitiesList.setOnItemClickListener { _, _, position, _ ->
            val city = favoriteCities[position]
            showCityOptions(city)
        }
    }

    override fun onResume() {
        super.onResume()
        loadFavoriteCities()
    }

    private fun loadFavoriteCities() {
        favoriteCities.clear()
        favoriteCities.addAll(databaseHelper.getFavoriteCities())

        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        } else {
            adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, favoriteCities)
            favoriteCitiesList.adapter = adapter
        }

        if (favoriteCities.isEmpty()) {
            Toast.makeText(this, "No favorite cities saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addFavoriteCity(city: String) {
        if (favoriteCities.contains(city)) {
            Toast.makeText(this, "City is already in favorites", Toast.LENGTH_SHORT).show()
            return
        }

        databaseHelper.addFavoriteCity(city) // Save to database
        favoriteCities.add(city)
        adapter.notifyDataSetChanged() // Refresh UI
        cityInput.text.clear()
        Toast.makeText(this, "Added $city to favorites", Toast.LENGTH_SHORT).show()
    }

    private fun clearAllFavorites() {
        if (favoriteCities.isEmpty()) {
            Toast.makeText(this, "No favorite cities to clear", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Clear Favorites")
            .setMessage("Are you sure you want to delete all favorite cities?")
            .setPositiveButton("Yes") { _, _ ->
                databaseHelper.clearFavoriteCities() // Delete from DB
                favoriteCities.clear()
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Favorites cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showCityOptions(city: String) {
        val options = arrayOf("Check Weather", "Remove from Favorites")
        AlertDialog.Builder(this)
            .setTitle(city)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openWeather(city)
                    1 -> removeFavoriteCity(city)
                }
            }
            .show()
    }

    private fun openWeather(city: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("city_name", city)
        startActivity(intent)
    }

    private fun removeFavoriteCity(city: String) {
        databaseHelper.deleteFavoriteCity(city)
        favoriteCities.remove(city)
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "$city removed from favorites", Toast.LENGTH_SHORT).show()
    }
}
