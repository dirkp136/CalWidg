package de.dirk.kalender.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider

object WidgetKeys {
    val MONTH_OFFSET = intPreferencesKey("month_offset")
    val TOKEN = longPreferencesKey("token")

    /** Appearance frozen per widget when it is placed. */
    val STYLE_SET = booleanPreferencesKey("style_set")
    val STYLE_MODE = intPreferencesKey("style_mode")
    val STYLE_GLASS = booleanPreferencesKey("style_glass")
}

/** Appearance of one widget instance: mode 0 = follow system, 1 = light, 2 = dark. */
data class Style(val mode: Int = 0, val glass: Boolean = false)

/** Provided per widget composition, so widgets with different styles never mix. */
val LocalStyle = staticCompositionLocalOf { Style() }

/** Palette: solid card or glass, each in light/dark (or following the system). */
object C {
    @Composable
    private fun pick(day: Long, night: Long, glassDay: Long, glassNight: Long): ColorProvider {
        val st = LocalStyle.current
        val d = if (st.glass) glassDay else day
        val n = if (st.glass) glassNight else night
        return when (st.mode) {
            1 -> ColorProvider(Color(d))
            2 -> ColorProvider(Color(n))
            else -> ColorProvider(day = Color(d), night = Color(n))
        }
    }

    val card: ColorProvider @Composable get() = pick(0xFFFFFFFF, 0xFF1C1C1E, 0x00000000, 0x00000000)
    val text: ColorProvider @Composable get() = pick(0xFF111114, 0xFFF2F2F5, 0xFF111114, 0xFFFFFFFF)
    val muted: ColorProvider @Composable get() = pick(0xFF8E8E93, 0xFF8E8E93, 0xB3222228, 0xBFFFFFFF)
    val faint: ColorProvider @Composable get() = pick(0xFFB9B9BE, 0xFF5C5C61, 0x66222228, 0x66FFFFFF)
    val red: ColorProvider @Composable get() = pick(0xFFE5484D, 0xFFFF6369, 0xFFD62F35, 0xFFFF8F93)
    val blue: ColorProvider @Composable get() = pick(0xFF3B82F6, 0xFF4F8FF7, 0xFF2F6FE4, 0xFF82B4FF)
    val week: ColorProvider @Composable get() = pick(0xFFEEEEF1, 0xFF2A2A2D, 0x33FFFFFF, 0x1FFFFFFF)
    val chip: ColorProvider @Composable get() = pick(0xFFF2F2F4, 0xFF2C2C2E, 0x4DFFFFFF, 0x2EFFFFFF)
    val white = ColorProvider(Color.White)

    private val heatDay = listOf(0xFFFFF7DB, 0xFFFDEDA9, 0xFFF9D682, 0xFFF3B36B)
    private val heatNight = listOf(0xFF332C17, 0xFF4A3D18, 0xFF5E4519, 0xFF6E3F1A)
    private val heatGlassDay = listOf(0x66FFF3C4, 0x8CFDE08A, 0xB3F9C760, 0xD9F3A04B)
    private val heatGlassNight = listOf(0x40FFD866, 0x66FFC94D, 0x8CFFB340, 0xB3FF8C33)

    /** Busyness color for a number of events, null when free. */
    @Composable
    fun heat(count: Int): ColorProvider? = if (count <= 0) null else minOf(count, 4).let {
        pick(heatDay[it - 1], heatNight[it - 1], heatGlassDay[it - 1], heatGlassNight[it - 1])
    }

    val isGlass: Boolean @Composable get() = LocalStyle.current.glass

    /** Background drawable for glass mode, matching the light/dark choice. */
    val glassBg: Int @Composable get() = when (LocalStyle.current.mode) {
        1 -> de.dirk.kalender.R.drawable.glass_bg_light
        2 -> de.dirk.kalender.R.drawable.glass_bg_dark
        else -> de.dirk.kalender.R.drawable.glass_bg
    }

    fun of(argb: Int) = ColorProvider(Color(argb))
}
