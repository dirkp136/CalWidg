package de.dirk.kalender.data

import android.content.Context
import de.dirk.kalender.R
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import kotlin.math.roundToInt

data class DayWx(val max: Int, val min: Int, val code: Int)
data class Weather(val now: Int, val nowCode: Int, val isDay: Boolean, val days: Map<LocalDate, DayWx>)
data class Place(val name: String, val region: String, val lat: Double, val lon: Double)

/** Weather from Open-Meteo (no API key). Cached in prefs, widgets only read the cache. */
object WeatherRepo {

    private const val MAX_AGE_MS = 12 * 3600_000L

    fun cached(ctx: Context): Weather? {
        val p = Prefs(ctx)
        if (!p.weatherEnabled) return null
        if (System.currentTimeMillis() - p.weatherFetchedAt > MAX_AGE_MS) return null
        return p.weatherJson?.let { runCatching { parse(it) }.getOrNull() }
    }

    /** Fetches fresh data; returns false on network errors (cache stays untouched). */
    fun refresh(ctx: Context): Boolean {
        val p = Prefs(ctx)
        if (!p.weatherEnabled) return true
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${p.lat}&longitude=${p.lon}" +
            "&current=temperature_2m,weather_code,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
            "&timezone=auto&forecast_days=7"
        val body = get(url) ?: return false
        if (runCatching { parse(body) }.isFailure) return false
        p.weatherJson = body
        p.weatherFetchedAt = System.currentTimeMillis()
        return true
    }

    fun searchPlaces(query: String): List<Place> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val body = get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=8&language=de") ?: return emptyList()
        val arr = JSONObject(body).optJSONArray("results") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Place(
                o.getString("name"),
                listOfNotNull(o.optString("admin1").takeIf { it.isNotBlank() }, o.optString("country").takeIf { it.isNotBlank() }).joinToString(", "),
                o.getDouble("latitude"), o.getDouble("longitude")
            )
        }
    }

    private fun parse(json: String): Weather {
        val o = JSONObject(json)
        val cur = o.getJSONObject("current")
        val d = o.getJSONObject("daily")
        val t = d.getJSONArray("time")
        val days = (0 until t.length()).associate { i ->
            LocalDate.parse(t.getString(i)) to DayWx(
                d.getJSONArray("temperature_2m_max").getDouble(i).roundToInt(),
                d.getJSONArray("temperature_2m_min").getDouble(i).roundToInt(),
                d.getJSONArray("weather_code").getInt(i),
            )
        }
        return Weather(
            cur.getDouble("temperature_2m").roundToInt(),
            cur.getInt("weather_code"),
            cur.getInt("is_day") == 1,
            days
        )
    }

    private fun get(url: String): String? = runCatching {
        (URL(url).openConnection() as HttpURLConnection).run {
            connectTimeout = 10_000; readTimeout = 10_000
            try { if (responseCode == 200) inputStream.bufferedReader().readText() else null } finally { disconnect() }
        }
    }.getOrNull()

    /** WMO weather code -> icon. */
    fun icon(code: Int, day: Boolean = true): Int = when (code) {
        0 -> if (day) R.drawable.wx_sun else R.drawable.wx_moon
        1, 2 -> if (day) R.drawable.wx_partly_day else R.drawable.wx_partly_night
        3 -> R.drawable.wx_cloud
        45, 48 -> R.drawable.wx_fog
        in 51..67, in 80..82 -> R.drawable.wx_rain
        in 71..77, 85, 86 -> R.drawable.wx_snow
        in 95..99 -> R.drawable.wx_thunder
        else -> R.drawable.wx_cloud
    }
}
