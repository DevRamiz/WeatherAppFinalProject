package com.example.finalproject

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createWeatherTable = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            city TEXT NOT NULL,
            date TEXT NOT NULL,
            temperature REAL NOT NULL,
            feels_like REAL NOT NULL,
            min_temp REAL NOT NULL,
            max_temp REAL NOT NULL,
            humidity INTEGER NOT NULL,
            wind_speed REAL NOT NULL,
            pressure INTEGER NOT NULL,
            description TEXT NOT NULL,
            unit TEXT DEFAULT '°C' NOT NULL
        )
        """.trimIndent()

        val createFavoritesTable = """
        CREATE TABLE IF NOT EXISTS favorite_locations (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            city TEXT UNIQUE NOT NULL
        )
        """.trimIndent()

        db.execSQL(createWeatherTable)
        db.execSQL(createFavoritesTable)
        Log.d("DatabaseHelper", "Tables created successfully")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < DATABASE_VERSION) {
            try {
                db.execSQL("DROP TABLE IF EXISTS favorite_locations")
                db.execSQL("CREATE TABLE IF NOT EXISTS favorite_locations (id INTEGER PRIMARY KEY AUTOINCREMENT, city TEXT UNIQUE NOT NULL)")
                Log.d("DatabaseHelper", "Database upgraded and favorite_locations recreated")
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error upgrading database", e)
            }
        }
    }

    fun getWeatherByDate(date: String): List<Pair<Int, String>> {
        val db = readableDatabase
        val searchResults = mutableListOf<Pair<Int, String>>()
        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery(
                "SELECT id, city, date, temperature, unit FROM $TABLE_NAME WHERE date LIKE ? ORDER BY id DESC",
                arrayOf("$date%")
            )

            while (cursor.moveToNext()) {
                val id = cursor.getInt(0)
                val record = "📍 ${cursor.getString(1)} - ${cursor.getString(2)}\n" +
                        "🌡 ${cursor.getDouble(3)}${cursor.getString(4)}"
                searchResults.add(id to record)
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error retrieving history by date", e)
        } finally {
            cursor?.close()
            db.close()
        }

        return searchResults
    }

    fun addWeatherRecord(
        city: String, date: String, temperature: Double, feelsLike: Double,
        minTemp: Double, maxTemp: Double, humidity: Int, windSpeed: Double,
        pressure: Int, description: String, isFahrenheit: Boolean
    ) {
        val db = writableDatabase
        val unit = if (isFahrenheit) "°F" else "°C"
        val values = ContentValues().apply {
            put("city", city)
            put("date", date)
            put("temperature", temperature)
            put("feels_like", feelsLike)
            put("min_temp", minTemp)
            put("max_temp", maxTemp)
            put("humidity", humidity)
            put("wind_speed", windSpeed)
            put("pressure", pressure)
            put("description", description)
            put("unit", unit)
        }

        try {
            db.insertOrThrow(TABLE_NAME, null, values)
            Log.d("DatabaseHelper", "Weather record added for $city")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error inserting weather record", e)
        } finally {
            db.close()
        }
    }

    fun addFavoriteCity(city: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("city", city)
        }

        try {
            val result = db.insertWithOnConflict("favorite_locations", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            if (result == -1L) {
                Log.e("DatabaseHelper", "City already exists in favorites: $city")
            } else {
                Log.d("DatabaseHelper", "City added to favorites: $city")
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error inserting favorite city", e)
        } finally {
            db.close()
        }
    }

    fun getFavoriteCities(): List<String> {
        val db = readableDatabase
        val cities = mutableListOf<String>()
        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery("SELECT city FROM favorite_locations ORDER BY city ASC", null)
            while (cursor.moveToNext()) {
                cities.add(cursor.getString(0))
            }
            Log.d("DatabaseHelper", "Retrieved ${cities.size} favorite cities")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error retrieving favorite cities", e)
        } finally {
            cursor?.close()
            db.close()
        }

        return cities
    }

    fun deleteFavoriteCity(city: String) {
        val db = writableDatabase
        try {
            val rowsDeleted = db.delete("favorite_locations", "city = ?", arrayOf(city))
            Log.d("DatabaseHelper", "Deleted $rowsDeleted rows for city: $city")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error deleting favorite city", e)
        } finally {
            db.close()
        }
    }

    fun clearFavoriteCities() {
        val db = writableDatabase
        try {
            db.execSQL("DELETE FROM favorite_locations")
            db.execSQL("VACUUM") // Reset auto-increment
            Log.d("DatabaseHelper", "Favorite locations cleared.")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error clearing favorite locations", e)
        } finally {
            db.close()
        }
    }

    fun getWeatherHistory(): List<Pair<Int, String>> {
        val db = readableDatabase
        val historyList = mutableListOf<Pair<Int, String>>()
        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery(
                "SELECT id, city, date, temperature, unit FROM $TABLE_NAME ORDER BY id DESC",
                null
            )

            while (cursor.moveToNext()) {
                val id = cursor.getInt(0)
                val record = "📍 ${cursor.getString(1)} - ${cursor.getString(2)}\n" +
                        "🌡 ${cursor.getDouble(3)}${cursor.getString(4)}"
                historyList.add(id to record)
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error retrieving history", e)
        } finally {
            cursor?.close()
            db.close()
        }

        return historyList
    }

    fun clearWeatherHistory() {
        val db = writableDatabase
        try {
            db.execSQL("DELETE FROM $TABLE_NAME")
            db.execSQL("VACUUM") // Reset auto-increment
            Log.d("DatabaseHelper", "Weather history cleared.")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error clearing weather history", e)
        } finally {
            db.close()
        }
    }

    companion object {
        const val DATABASE_NAME = "WeatherHistoryDB"
        const val DATABASE_VERSION = 4 // Updated version to force upgrade
        const val TABLE_NAME = "weather_history"
    }
}
