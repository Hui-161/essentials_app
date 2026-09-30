package com.sameerasw.essentials.ui.features.weather

import androidx.compose.ui.graphics.Color
import com.sameerasw.essentials.weather.model.WeatherCondition
import com.sameerasw.essentials.weather.model.WeatherSnapshot

// Same near-black base as the island; only the glow and accent follow the conditions and time of day.
class WeatherPalette(
    val glow: Color,
    val accent: Color,
    val base: Color = Color.Black,
    val card: Color = Color.White.copy(alpha = 0.09f),
    val onBase: Color = Color.White,
    val onBaseMuted: Color = Color.White.copy(alpha = 0.66f),
) {
    fun withAccent(color: Color) = WeatherPalette(glow, color, base, card, onBase, onBaseMuted)

    companion object {
        val Neutral = WeatherPalette(glow = Color(0xFF1E1E1E), accent = Color.White)

        fun from(snapshot: WeatherSnapshot?): WeatherPalette {
            if (snapshot == null) return Neutral
            val day = snapshot.isDay
            return when (snapshot.condition) {
                WeatherCondition.CLEAR ->
                    if (day) WeatherPalette(Color(0xFF12426E), Color(0xFFFFC857)) else WeatherPalette(Color(0xFF0B1230), Color(0xFFB8C4FF))
                WeatherCondition.PARTLY_CLOUDY ->
                    if (day) WeatherPalette(Color(0xFF1D3F58), Color(0xFFFFD27F)) else WeatherPalette(Color(0xFF111833), Color(0xFFC3CCF5))
                WeatherCondition.CLOUDY -> WeatherPalette(Color(0xFF2B323B), Color(0xFFB0BEC5))
                WeatherCondition.FOG -> WeatherPalette(Color(0xFF2C3136), Color(0xFFCFD8DC))
                WeatherCondition.DRIZZLE -> WeatherPalette(Color(0xFF16293A), Color(0xFF7CC4FF))
                WeatherCondition.RAIN -> WeatherPalette(Color(0xFF142536), Color(0xFF6EC1FF))
                WeatherCondition.HEAVY_RAIN -> WeatherPalette(Color(0xFF0E1C2B), Color(0xFF4FA3E3))
                WeatherCondition.SLEET -> WeatherPalette(Color(0xFF1F2B38), Color(0xFFBBD9F2))
                WeatherCondition.SNOW -> WeatherPalette(Color(0xFF23303D), Color(0xFFDCEBFF))
                WeatherCondition.HAIL -> WeatherPalette(Color(0xFF1B2631), Color(0xFFB7D4EA))
                WeatherCondition.THUNDERSTORM -> WeatherPalette(Color(0xFF1C1433), Color(0xFFFFE066))
                WeatherCondition.UNKNOWN -> Neutral
            }
        }
    }
}
