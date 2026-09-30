package com.sameerasw.essentials.ui.features.weather

import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.foundation.layout.PaddingValues
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.Shapes
import com.sameerasw.essentials.ui.theme.Typography
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.weather.WeatherFormat
import com.sameerasw.essentials.weather.WeatherRepository
import com.sameerasw.essentials.weather.effects.DeviceWeatherHaptics
import com.sameerasw.essentials.weather.effects.WeatherEffectSpec
import com.sameerasw.essentials.weather.effects.WeatherEffects
import com.sameerasw.essentials.weather.model.DailyForecast
import com.sameerasw.essentials.weather.model.TemperatureUnit
import com.sameerasw.essentials.weather.model.WeatherAlert
import com.sameerasw.essentials.weather.model.WeatherError
import com.sameerasw.essentials.weather.model.WeatherSnapshot
import com.sameerasw.essentials.weather.provider.WeatherProviders
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WeatherDetailSheet(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsRepository(context) }
    val state by WeatherRepository.state.collectAsState()
    val snapshot = state.snapshot
    val unit = remember { WeatherFormat.unitFor(settings.getWeatherUnits()) }
    val materialYou = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context).primary else null
    val palette = remember(snapshot?.condition, snapshot?.isDay, materialYou) {
        WeatherPalette.from(snapshot).let { base -> materialYou?.let(base::withAccent) ?: base }
    }
    val effects = remember { settings.isIslandWeatherEffectsEnabled() && !DeviceUtils.isPowerSaveMode(context) }
    val effectSpec = remember(effects, snapshot?.condition, snapshot?.isDay, snapshot?.windKph) {
        snapshot?.takeIf { effects }?.let(WeatherEffectSpec::from) ?: WeatherEffectSpec.None
    }
    val effectHaptics = remember(context) { DeviceWeatherHaptics(context).takeIf { settings.isIslandWeatherHapticsEnabled() } }

    LaunchedEffect(Unit) {
        WeatherRepository.ensureLoaded(context)
        if (WeatherRepository.isStale(context)) WeatherRepository.refresh(context)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = palette.accent,
            onPrimary = Color.Black,
            surface = palette.base,
            onSurface = palette.onBase,
            onSurfaceVariant = palette.onBaseMuted,
            background = palette.base,
            onBackground = palette.onBase,
        ),
        typography = Typography,
        shapes = Shapes,
    ) {
        val sheetShape = BottomSheetDefaults.ExpandedShape
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.Transparent,
            shape = sheetShape,
            dragHandle = null,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            modifier = Modifier.statusBarsPadding(),
        ) {
            Box(Modifier.fillMaxWidth().fillMaxHeight().clip(sheetShape)) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Brush.verticalGradient(0f to palette.glow, 0.6f to palette.base, 1f to palette.base)),
                )
                if (!effectSpec.isEmpty) {
                    WeatherEffects(spec = effectSpec, modifier = Modifier.matchParentSize(), haptics = effectHaptics)
                }
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(top = 36.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    if (snapshot == null) {
                        Spacer(Modifier.height(80.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            if (state.loading) LoadingIndicator() else Text(errorLabel(context, state.error), color = palette.onBaseMuted)
                        }
                    } else {
                        Header(snapshot, unit, palette, Modifier.padding(horizontal = SIDE_PADDING))
                        snapshot.activeAlerts().sortedByDescending { it.severity.ordinal }
                            .forEach { AlertCard(it, palette, Modifier.padding(horizontal = SIDE_PADDING)) }
                        HourlySection(snapshot, unit, palette)
                        snapshot.daily.orEmpty().takeIf { it.isNotEmpty() }
                            ?.let { DailySection(it, unit, palette, Modifier.padding(horizontal = SIDE_PADDING)) }
                        DetailsSection(snapshot, unit, palette, Modifier.padding(horizontal = SIDE_PADDING))
                        SunSection(snapshot, palette, Modifier.padding(horizontal = SIDE_PADDING))
                        Footer(
                            modifier = Modifier.padding(horizontal = SIDE_PADDING),
                            snapshot = snapshot,
                            loading = state.loading,
                            error = state.error,
                            palette = palette,
                            onRefresh = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                scope.launch { WeatherRepository.refresh(context, force = true) }
                            },
                        )
                    }
                }
                BottomSheetDefaults.DragHandle(
                    color = palette.onBaseMuted,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}

private val SIDE_PADDING = 20.dp

@OptIn(ExperimentalTextApi::class)
private val TemperatureFont = FontFamily(
    Font(
        R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.width(140f),
            FontVariation.weight(FontWeight.Normal.weight),
            FontVariation.Setting("ROND", 100f),
        ),
    ),
)

@Composable
private fun Header(snapshot: WeatherSnapshot, unit: TemperatureUnit, palette: WeatherPalette, modifier: Modifier) {
    val place = listOf(snapshot.locationName, snapshot.region).filter { it.isNotBlank() }.joinToString(", ")
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (place.isNotBlank()) {
            AssistChip(
                onClick = {},
                label = { Text(place, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium) },
                leadingIcon = { Icon(painterResource(R.drawable.rounded_location_on_24), null, modifier = Modifier.size(20.dp)) },
                shape = CircleShape,
                border = null,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = palette.card,
                    labelColor = palette.onBase,
                    leadingIconContentColor = palette.accent,
                ),
            )
        }
        Text(
            WeatherFormat.temperature(snapshot.tempC, unit),
            color = palette.onBase,
            fontFamily = TemperatureFont,
            fontSize = 150.sp,
            lineHeight = 150.sp,
            letterSpacing = (-4).sp,
            maxLines = 1,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                painterResource(WeatherFormat.icon(snapshot.condition, snapshot.isDay)),
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(32.dp),
            )
            Text(snapshot.conditionText, color = palette.onBase, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            listOf(
                stringResource(R.string.weather_high_low, WeatherFormat.temperature(snapshot.highC, unit), WeatherFormat.temperature(snapshot.lowC, unit)),
                stringResource(R.string.weather_feels_like, WeatherFormat.temperature(snapshot.feelsLikeC, unit)),
            ).joinToString(" · "),
            color = palette.onBaseMuted,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun AlertCard(alert: WeatherAlert, palette: WeatherPalette, modifier: Modifier) {
    val context = LocalContext.current
    val alertColor = MaterialTheme.colorScheme.error
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(alertColor.copy(alpha = 0.22f))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(R.drawable.rounded_warning_24), null, tint = alertColor, modifier = Modifier.size(24.dp))
            Text(alert.event, color = palette.onBase, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        alert.expiresMillis?.let {
            Text(stringResource(R.string.weather_alert_until, formatTime(context, it)), color = palette.onBaseMuted, style = MaterialTheme.typography.bodyMedium)
        }
        if (alert.headline != alert.event) Text(alert.headline, color = palette.onBase, style = MaterialTheme.typography.bodyMedium)
        if (alert.description.isNotBlank()) Text(alert.description, color = palette.onBaseMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HourlySection(snapshot: WeatherSnapshot, unit: TemperatureUnit, palette: WeatherPalette) {
    if (snapshot.hourly.isEmpty()) return
    val context = LocalContext.current
    val hours = snapshot.hourly
    val carouselState = rememberCarouselState { hours.size }
    Column {
        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 104.dp,
            itemSpacing = 4.dp,
            contentPadding = PaddingValues(horizontal = SIDE_PADDING),
            modifier = Modifier.fillMaxWidth().height(176.dp),
        ) { index ->
            val hour = hours[index]
            Column(
                Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge)
                    .background(palette.card)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatHour(context, hour.timeMillis), color = palette.onBaseMuted, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                Icon(
                    painterResource(WeatherFormat.icon(hour.condition, hour.isDay)),
                    null,
                    tint = palette.accent,
                    modifier = Modifier.size(32.dp),
                )
                Text(WeatherFormat.temperature(hour.tempC, unit), color = palette.onBase, style = MaterialTheme.typography.titleLarge, maxLines = 1, softWrap = false)
                Text(
                    if (hour.chanceOfRain > 0) "${hour.chanceOfRain}%" else " ",
                    color = palette.accent,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun DailySection(days: List<DailyForecast>, unit: TemperatureUnit, palette: WeatherPalette, modifier: Modifier) {
    val low = days.minOf { it.lowC }
    val high = days.maxOf { it.highC }
    val span = (high - low).coerceAtLeast(1.0)
    Column(modifier) {
        RoundedCardContainer(spacing = 2.dp, cornerRadius = 28.dp) {
            days.forEachIndexed { index, day ->
                Surface(color = palette.card, shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            if (index == 0) stringResource(R.string.weather_detail_today) else dayName(day.dayMillis),
                            color = palette.onBase,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(64.dp),
                            maxLines = 1,
                        )
                        Icon(painterResource(WeatherFormat.icon(day.condition, true)), null, tint = palette.accent, modifier = Modifier.size(26.dp))
                        Text(
                            if (day.chanceOfRain > 0) "${day.chanceOfRain}%" else "",
                            color = palette.accent,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(36.dp),
                        )
                        Text(WeatherFormat.temperature(day.lowC, unit), color = palette.onBaseMuted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(38.dp), textAlign = TextAlign.End)
                        Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))) {
                            val start = ((day.lowC - low) / span).toFloat().coerceIn(0f, 1f)
                            val end = ((day.highC - low) / span).toFloat().coerceIn(start, 1f)
                            Row(Modifier.matchParentSize()) {
                                if (start > 0f) Spacer(Modifier.weight(start))
                                Box(Modifier.weight((end - start).coerceAtLeast(0.05f)).fillMaxHeight().clip(CircleShape).background(palette.accent))
                                if (end < 1f) Spacer(Modifier.weight(1f - end))
                            }
                        }
                        Text(WeatherFormat.temperature(day.highC, unit), color = palette.onBase, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(38.dp))
                    }
                }
            }
        }
    }
}

private class Detail(val icon: Int, val label: Int, val value: String)

@Composable
private fun DetailsSection(snapshot: WeatherSnapshot, unit: TemperatureUnit, palette: WeatherPalette, modifier: Modifier) {
    val imperial = unit == TemperatureUnit.FAHRENHEIT
    val extras = snapshot.extras
    val details = buildList {
        add(Detail(R.drawable.rounded_water_drop_24, R.string.weather_detail_humidity, "${snapshot.humidity}%"))
        add(
            Detail(
                R.drawable.rounded_air_24,
                R.string.weather_detail_wind,
                WeatherFormat.wind(snapshot.windKph, unit) + (extras?.windDirectionDeg?.let { " ${compass(it)}" } ?: ""),
            ),
        )
        extras?.windGustKph?.let { add(Detail(R.drawable.rounded_air_24, R.string.weather_detail_gusts, WeatherFormat.wind(it, unit))) }
        add(Detail(R.drawable.rounded_rainy_24, R.string.weather_detail_rain_chance, "${snapshot.chanceOfRain}%"))
        extras?.precipitationMm?.let {
            add(Detail(R.drawable.rounded_rainy_24, R.string.weather_detail_precipitation, if (imperial) "%.2f in".format(Locale.US, it / 25.4) else "%.1f mm".format(Locale.US, it)))
        }
        extras?.uvIndex?.let { add(Detail(R.drawable.rounded_wb_sunny_24, R.string.weather_detail_uv, it.roundToInt().toString())) }
        extras?.pressureHpa?.let {
            add(Detail(R.drawable.rounded_cloud_24, R.string.weather_detail_pressure, if (imperial) "%.2f inHg".format(Locale.US, it * 0.02953) else "${it.roundToInt()} hPa"))
        }
        extras?.visibilityKm?.let {
            add(Detail(R.drawable.rounded_visibility_24, R.string.weather_detail_visibility, if (imperial) "%.1f mi".format(Locale.US, it / 1.609344) else "%.1f km".format(Locale.US, it)))
        }
        extras?.dewPointC?.let { add(Detail(R.drawable.rounded_water_drop_24, R.string.weather_detail_dew_point, WeatherFormat.temperature(it, unit))) }
        extras?.cloudCover?.let { add(Detail(R.drawable.rounded_cloud_24, R.string.weather_detail_cloud_cover, "$it%")) }
    }
    Column(modifier) {
        Surface(color = palette.card, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
                details.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { DetailTile(it, palette, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTile(detail: Detail, palette: WeatherPalette, modifier: Modifier) {
    Row(
        modifier.padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(painterResource(detail.icon), null, tint = palette.accent, modifier = Modifier.size(26.dp))
        Column {
            Text(stringResource(detail.label), color = palette.onBaseMuted, style = MaterialTheme.typography.labelMedium)
            Text(detail.value, color = palette.onBase, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

@Composable
private fun SunSection(snapshot: WeatherSnapshot, palette: WeatherPalette, modifier: Modifier) {
    val rise = snapshot.extras?.sunriseMillis ?: return
    val set = snapshot.extras?.sunsetMillis ?: return
    val context = LocalContext.current
    Column(modifier) {
        RoundedCardContainer(spacing = 2.dp, cornerRadius = 28.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                SunTile(R.string.weather_detail_sunrise, formatTime(context, rise), palette, Modifier.weight(1f))
                SunTile(R.string.weather_detail_sunset, formatTime(context, set), palette, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SunTile(label: Int, time: String, palette: WeatherPalette, modifier: Modifier) {
    Surface(color = palette.card, shape = MaterialTheme.shapes.extraSmall, modifier = modifier) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(label), color = palette.onBaseMuted, style = MaterialTheme.typography.labelMedium)
            Text(time, color = palette.onBase, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun Footer(
    modifier: Modifier,
    snapshot: WeatherSnapshot,
    loading: Boolean,
    error: WeatherError?,
    palette: WeatherPalette,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(snapshot.updatedAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000L)
        }
    }
    val age = DateUtils.getRelativeTimeSpanString(
        snapshot.updatedAt,
        maxOf(now, snapshot.updatedAt),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = error?.let { errorLabel(context, it) } ?: "${WeatherProviders.byId(snapshot.providerId).displayName} - $age",
            color = palette.onBaseMuted,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (loading) {
            LoadingIndicator(Modifier.size(28.dp))
        } else {
            Icon(
                painterResource(R.drawable.rounded_refresh_24),
                contentDescription = null,
                tint = palette.onBase,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onRefresh).padding(8.dp).size(20.dp),
            )
        }
    }
}

private fun errorLabel(context: Context, error: WeatherError?): String {
    val base = context.getString(
        when (error) {
            WeatherError.MissingApiKey -> R.string.weather_error_missing_key
            WeatherError.InvalidApiKey -> R.string.weather_error_invalid_key
            WeatherError.LocationPermission -> R.string.weather_error_location_permission
            WeatherError.NoLocation -> R.string.weather_error_no_location
            WeatherError.Network -> R.string.weather_error_network
            is WeatherError.Unknown, null -> R.string.weather_error_unknown
        },
    )
    val detail = (error as? WeatherError.Unknown)?.message?.takeIf { it.isNotBlank() }
    return if (detail != null) "$base: $detail" else base
}

private fun compass(degrees: Double): String {
    val points = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return points[(((degrees % 360 + 360) % 360) / 45.0).roundToInt() % 8]
}

private fun dayName(millis: Long): String = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(millis))

private fun formatTime(context: Context, millis: Long): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}

private fun formatHour(context: Context, millis: Long): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH" else "ha"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis)).lowercase(Locale.getDefault())
}
