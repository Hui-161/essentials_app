package com.sameerasw.essentials.weather.provider

import com.sameerasw.essentials.weather.model.AlertSeverity
import com.sameerasw.essentials.weather.model.CityResult
import com.sameerasw.essentials.weather.model.HourlyForecast
import com.sameerasw.essentials.weather.model.WeatherAlert
import com.sameerasw.essentials.weather.model.WeatherCondition
import com.sameerasw.essentials.weather.model.WeatherLocation
import com.sameerasw.essentials.weather.model.WeatherSnapshot
import org.json.JSONException
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

class VisualCrossingProvider : WeatherProvider {
    override val id = "visualcrossing"
    override val displayName = "Visual Crossing"
    override val requiresApiKey = true
    override val signupUrl = "https://www.visualcrossing.com/sign-up"

    override suspend fun fetch(location: WeatherLocation, apiKey: String?): WeatherSnapshot {
        val key = apiKey?.trim().orEmpty()
        if (key.isEmpty()) throw WeatherProviderException(WeatherProviderException.Reason.INVALID_KEY)
        val url = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/" +
            "${location.latitude},${location.longitude}/next2days?unitGroup=metric&include=current,hours,days,alerts" +
            "&contentType=json&key=${ProviderHttp.encode(key)}"
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
        val current = json.getJSONObject("currentConditions")
        val days = json.getJSONArray("days")
        val today = days.getJSONObject(0)

        val hourly = buildList {
            for (d in 0 until days.length()) {
                val hours = days.getJSONObject(d).optJSONArray("hours") ?: continue
                for (h in 0 until hours.length()) {
                    val hour = hours.getJSONObject(h)
                    val time = hour.getLong("datetimeEpoch") * 1000L
                    if (time + HOUR_MS <= now) continue
                    val icon = hour.optString("icon")
                    add(
                        HourlyForecast(
                            timeMillis = time,
                            tempC = hour.getDouble("temp"),
                            condition = conditionFor(icon),
                            isDay = !icon.endsWith("-night"),
                            chanceOfRain = hour.optDouble("precipprob", 0.0).toInt(),
                        ),
                    )
                }
            }
        }.take(HOURLY_COUNT)

        val alertArray = json.optJSONArray("alerts")
        val alerts = buildList {
            if (alertArray != null) {
                for (i in 0 until alertArray.length()) {
                    val a = alertArray.getJSONObject(i)
                    val event = a.optString("event").ifBlank { a.optString("headline") }
                    add(
                        WeatherAlert(
                            id = "$id:${a.optString("id")}:${a.optString("onset")}:$event".hashCode().toString(),
                            event = event,
                            headline = a.optString("headline").ifBlank { event },
                            severity = AlertSeverity.UNKNOWN,
                            description = a.optString("description").trim(),
                            effectiveMillis = parseTime(a.optString("onsetEpoch").ifBlank { null }, a.optString("onset")),
                            expiresMillis = parseTime(a.optString("endsEpoch").ifBlank { null }, a.optString("ends")),
                        ),
                    )
                }
            }
        }.distinctBy { it.id }

        val icon = current.optString("icon")
        val temp = current.getDouble("temp")
        return WeatherSnapshot(
            locationName = place?.first.orEmpty(),
            region = place?.second.orEmpty(),
            tempC = temp,
            feelsLikeC = current.optDouble("feelslike", temp),
            condition = conditionFor(icon),
            conditionText = current.optString("conditions"),
            isDay = !icon.endsWith("-night"),
            humidity = current.optDouble("humidity", 0.0).toInt(),
            windKph = current.optDouble("windspeed", 0.0),
            chanceOfRain = today.optDouble("precipprob", 0.0).toInt(),
            highC = today.optDouble("tempmax", temp),
            lowC = today.optDouble("tempmin", temp),
            hourly = hourly,
            alerts = alerts,
            updatedAt = now,
            providerId = id,
        )
    }

    private fun parseTime(epoch: String?, iso: String?): Long? {
        epoch?.toLongOrNull()?.let { return it * 1000L }
        if (iso.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(iso).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    // https://www.visualcrossing.com/resources/documentation/weather-api/defining-icon-set-in-the-weather-api/
    private fun conditionFor(icon: String): WeatherCondition = when {
        icon.startsWith("thunder") -> WeatherCondition.THUNDERSTORM
        icon.contains("snow") && icon.contains("rain") -> WeatherCondition.SLEET
        icon.contains("snow") -> WeatherCondition.SNOW
        icon.contains("sleet") || icon.contains("freezing") -> WeatherCondition.SLEET
        icon.contains("hail") -> WeatherCondition.HAIL
        icon.startsWith("showers") -> WeatherCondition.DRIZZLE
        icon.contains("rain") -> WeatherCondition.RAIN
        icon == "fog" -> WeatherCondition.FOG
        icon.startsWith("clear") -> WeatherCondition.CLEAR
        icon.startsWith("partly-cloudy") -> WeatherCondition.PARTLY_CLOUDY
        icon == "cloudy" || icon == "wind" -> WeatherCondition.CLOUDY
        else -> WeatherCondition.UNKNOWN
    }

    private companion object {
        const val HOUR_MS = 60 * 60 * 1000L
        const val HOURLY_COUNT = 8
    }
}
