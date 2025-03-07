package com.example.finalproject

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class WeatherHistoryActivity : AppCompatActivity() {
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var historyListView: ListView
    private lateinit var searchButton: Button
    private lateinit var resetHistoryButton: Button
    private lateinit var searchInput: EditText
    private lateinit var historyAdapter: ArrayAdapter<String>
    private var historyData: MutableList<Pair<Int, String>> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_history)

        databaseHelper = DatabaseHelper(this)
        historyListView = findViewById(R.id.historyList)
        searchButton = findViewById(R.id.searchButton)
        searchInput = findViewById(R.id.searchInput)
        resetHistoryButton = findViewById(R.id.resetHistoryButton)


        try {
            loadWeatherHistory()
        } catch (e: Exception) {
            Log.e("WeatherHistory", "Error loading history", e)
            Toast.makeText(this, "Error loading weather history", Toast.LENGTH_SHORT).show()
        }

        resetHistoryButton.setOnClickListener {
            showResetConfirmationDialog()
        }

        searchButton.setOnClickListener {
            val dateQuery = searchInput.text.toString().trim()
            if (dateQuery.isNotEmpty()) {
                searchWeatherByDate(dateQuery)
            } else {
                loadWeatherHistory()
            }
        }
    }

    private fun loadWeatherHistory() {
        historyData = databaseHelper.getWeatherHistory().toMutableList()
        if (historyData.isEmpty()) {
            return
        }

        val historyRecords = historyData.map { it.second }
        historyAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, historyRecords)
        historyListView.adapter = historyAdapter
    }

    private fun searchWeatherByDate(date: String) {
        val searchResults = databaseHelper.getWeatherByDate(date)

        if (searchResults.isEmpty()) {
            Toast.makeText(this, "No records found for this date", Toast.LENGTH_SHORT).show()
            return
        }

        historyAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, searchResults.map { it.second })
        historyListView.adapter = historyAdapter
    }
    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset History")
            .setMessage("Are you sure you want to delete all weather history?")
            .setPositiveButton("Yes") { _, _ ->
                databaseHelper.clearWeatherHistory()
                loadWeatherHistory()
                recreate()
                Toast.makeText(this, "History Reset Successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
