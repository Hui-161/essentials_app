package com.sameerasw.essentials.weather.provider

import com.sameerasw.essentials.weather.model.CityResult
import com.sameerasw.essentials.weather.model.HourlyForecast
import com.sameerasw.essentials.weather.model.WeatherCondition
import com.sameerasw.essentials.weather.model.WeatherLocation
import com.sameerasw.essentials.weather.model.WeatherSnapshot
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

class OpenMeteoProvider : WeatherProvider {
    override val id = "openmeteo"
    override val displayName = "Open-Meteo"
    override val requiresApiKey = false
    override val signupUrl: String? = null

    override suspend fun fetch(location: WeatherLocation, apiKey: String?): WeatherSnapshot {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m" +
            "&hourly=temperature_2m,precipitation_probability,weather_code,is_day" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&forecast_days=2&timezone=auto&wind_speed_unit=kmh&timeformat=unixtime"
        val json = JSONObject(ProviderHttp.get(url))
        val place = location.name?.let { it to "" } ?: ProviderHttp.reverseGeocode(location.latitude, location.longitude)
        return try {
            parse(json, place)
        } catch (e: JSONException) {
            throw WeatherProviderException(WeatherProviderException.Reason.BAD_RESPONSE, e.message)
        }
    }

    override suspend fun searchCities(query: String, apiKey: String?): List<CityResult> = ProviderHttp.searchCities(query)

    private fun parse(json: JSONObject, place: Pair<String, String>?): WeatherSnapshot {
        val now = System.currentTimeMillis()
        val current = json.getJSONObject("current")
        val hourlyJson = json.getJSONObject("hourly")
        val daily = json.getJSONObject("daily")

        val times = hourlyJson.getJSONArray("time")
        val temps = hourlyJson.getJSONArray("temperature_2m")
        val rain = hourlyJson.optJSONArray("precipitation_probability")
        val codes = hourlyJson.getJSONArray("weather_code")
        val day = hourlyJson.optJSONArray("is_day")
        val hourly = buildList {
            for (i in 0 until times.length()) {
                val time = times.getLong(i) * 1000L
                if (time + HOUR_MS <= now) continue
                add(
                    HourlyForecast(
                        timeMillis = time,
                        tempC = temps.getDouble(i),
                        condition = conditionFor(codes.optInt(i)),
                        isDay = day?.optInt(i, 1) != 0,
                        chanceOfRain = rain?.optInt(i) ?: 0,
                    ),
                )
            }
        }.take(HOURLY_COUNT)

        val code = current.optInt("weather_code")
        val temp = current.getDouble("temperature_2m")
        return WeatherSnapshot(
            locationName = place?.first.orEmpty(),
            region = place?.second.orEmpty(),
            tempC = temp,
            feelsLikeC = current.optDouble("apparent_temperature", temp),
            condition = conditionFor(code),
            conditionText = textFor(code),
            isDay = current.optInt("is_day", 1) == 1,
            humidity = current.optInt("relative_humidity_2m"),
            windKph = current.optDouble("wind_speed_10m"),
            chanceOfRain = daily.optJSONArray("precipitation_probability_max")?.optInt(0) ?: 0,
            highC = daily.getJSONArray("temperature_2m_max").getDouble(0),
            lowC = daily.getJSONArray("temperature_2m_min").getDouble(0),
            hourly = hourly,
            alerts = emptyList(),
            updatedAt = now,
            providerId = id,
        )
    }

    // https://open-meteo.com/en/docs (WMO weather interpretation codes)
    private fun conditionFor(code: Int): WeatherCondition = when (code) {
        0, 1 -> WeatherCondition.CLEAR
        2 -> WeatherCondition.PARTLY_CLOUDY
        3 -> WeatherCondition.CLOUDY
        45, 48 -> WeatherCondition.FOG
        51, 53, 55 -> WeatherCondition.DRIZZLE
        56, 57, 66, 67 -> WeatherCondition.SLEET
        61, 63, 80, 81 -> WeatherCondition.RAIN
        65, 82 -> WeatherCondition.HEAVY_RAIN
        71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW
        95, 96, 99 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.UNKNOWN
    }

    private fun textFor(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61 -> "Light rain"
        63 -> "Rain"
        65 -> "Heavy rain"
        66, 67 -> "Freezing rain"
        71 -> "Light snow"
        73 -> "Snow"
        75, 86 -> "Heavy snow"
        77 -> "Snow grains"
        80, 81 -> "Rain showers"
        82 -> "Violent rain showers"
        85 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> String.format(Locale.ROOT, "Weather %d", code)
    }

    private companion object {
        const val HOUR_MS = 60 * 60 * 1000L
        const val HOURLY_COUNT = 8
    }
}
