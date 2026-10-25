package de.dirk.kalender.data

import android.content.Context

/** Simple app settings backed by SharedPreferences. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** null = never configured yet (defaults get applied once permission is granted). */
    var selectedCalendars: Set<Long>?
        get() = sp.getStringSet("calendars", null)?.mapNotNull { it.toLongOrNull() }?.toSet()
        set(v) = sp.edit().putStringSet("calendars", v?.map { it.toString() }?.toSet()).apply()

    var weatherEnabled: Boolean
        get() = sp.getBoolean("weather_on", true)
        set(v) = sp.edit().putBoolean("weather_on", v).apply()

    var placeName: String
        get() = sp.getString("place", "Friedrichsdorf") ?: "Friedrichsdorf"
        set(v) = sp.edit().putString("place", v).apply()

    var lat: Double
        get() = sp.getString("lat", null)?.toDoubleOrNull() ?: 50.24962
        set(v) = sp.edit().putString("lat", v.toString()).apply()

    var lon: Double
        get() = sp.getString("lon", null)?.toDoubleOrNull() ?: 8.64281
        set(v) = sp.edit().putString("lon", v.toString()).apply()

    /** Widget appearance: 0 = follow system, 1 = light, 2 = dark. */
    var theme: Int
        get() = sp.getInt("theme", 0)
        set(v) = sp.edit().putInt("theme", v).apply()

    /** Translucent "glass" widget background instead of the solid card. */
    var glass: Boolean
        get() = sp.getBoolean("glass", false)
        set(v) = sp.edit().putBoolean("glass", v).apply()

    var weatherJson: String?
        get() = sp.getString("wx_json", null)
        set(v) = sp.edit().putString("wx_json", v).apply()

    var weatherFetchedAt: Long
        get() = sp.getLong("wx_at", 0L)
        set(v) = sp.edit().putLong("wx_at", v).apply()
}
