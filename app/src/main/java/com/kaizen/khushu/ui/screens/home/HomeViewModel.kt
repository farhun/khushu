@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.kaizen.khushu.ui.screens.home

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaizen.khushu.data.repository.extraPrayerTimingArabicLabel
import com.kaizen.khushu.data.repository.extraPrayerTimingShortLabel

import com.kaizen.khushu.data.repository.PrayerTimeRepository
import com.kaizen.khushu.data.repository.SettingsRepository
import com.kaizen.khushu.logic.PrayerManager
import com.kaizen.khushu.ui.screens.settings.SettingsViewModel
import com.kaizen.khushu.widget.PrayerWidgetProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Instant
import java.text.SimpleDateFormat
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter

import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import androidx.lifecycle.ViewModelProvider

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    private val prayerTimeRepository: PrayerTimeRepository,
    val prayerManager: PrayerManager,
    private val settingsViewModel: SettingsViewModel,
    private val appContext: Context,
) : ViewModel() {

    private val _previewTimeMillis = MutableStateFlow<Long?>(null)
    private val _isRefreshing = MutableStateFlow(false)
    private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

    private var refreshStartedAtMillis = 0L

    private suspend fun waitForMinimumRefreshDuration() {
        val remaining = MIN_REFRESH_DURATION_MS - (System.currentTimeMillis() - refreshStartedAtMillis)
        if (remaining > 0L) delay(remaining)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.settingsFlow,
        prayerManager.prayerState,
        _previewTimeMillis,
        _isRefreshing
    ) { settings, prayerState, previewTimeMillis, isRefreshing ->
        val usingPreviewTime = previewTimeMillis != null
        val previewTime = previewTimeMillis?.let { kotlinx.datetime.Instant.fromEpochMilliseconds(it) }
        val effectiveDate = Date(previewTimeMillis ?: System.currentTimeMillis())
        val isApiSource = prayerTimeRepository.usesApiSource(settings)

        if (prayerState == null) {
            return@combine HomeUiState(
                isRefreshing = isRefreshing,
                usingPreviewTime = usingPreviewTime,
                previewTime = previewTime,
                lastPrayerRefreshEpochMs = settings.lastPrayerRefreshEpochMs,
                locationLat = settings.locationLat,
                locationLng = settings.locationLng,
                calculationSource = if (isApiSource) CalculationSource.API else CalculationSource.LOCAL
            )
        }

        val effectivePrayerTimes = prayerTimeRepository.getEffectivePrayerDateTimes(
            date = effectiveDate,
            settings = settings
        ).mapValues { (_, value) -> kotlinx.datetime.Instant.fromEpochMilliseconds(value.time) }

        fun applyOffset(instant: kotlinx.datetime.Instant, minutes: Int): kotlinx.datetime.Instant {
            return kotlinx.datetime.Instant.fromEpochMilliseconds(instant.toEpochMilliseconds() + minutes * 60_000L)
        }

        val adjustedFajr = effectivePrayerTimes["Fajr"] ?: prayerState.allPrayers.fajr
        val adjustedDhuhr = effectivePrayerTimes["Dhuhr"] ?: prayerState.allPrayers.dhuhr
        val adjustedAsr = effectivePrayerTimes["Asr"] ?: prayerState.allPrayers.asr
        val adjustedMaghrib = effectivePrayerTimes["Maghrib"] ?: prayerState.allPrayers.maghrib
        val adjustedIsha = effectivePrayerTimes["Isha"] ?: prayerState.allPrayers.isha

        val extraPrayerTimes = runCatching {
            prayerTimeRepository.getExtraPrayerDateTimes(
                date = effectiveDate,
                settings = settings
            )
        }.getOrDefault(emptyMap())

        val sunriseInstant =
            extraPrayerTimes["SUNRISE"]?.let { kotlinx.datetime.Instant.fromEpochMilliseconds(it.time) }
                ?: prayerState.allPrayers.sunrise
        val sunsetInstant = adjustedMaghrib

        // --- NEW SUNPATH MATH: Center on Solar Noon (Zawal) ---
        // We define Solar Noon as the midpoint between Sunrise and Sunset.
        val solarNoonMs = (sunriseInstant.toEpochMilliseconds() + sunsetInstant.toEpochMilliseconds()) / 2L
        val dayMs = 24 * 60 * 60 * 1000L

        /**
         * Maps a timestamp to a 0..1 range representing a 24-hour window
         * centered EXACTLY on Solar Noon (t=0.5).
         */
        fun tToArc(ms: Long): Float {
            val offsetFromNoon = ms - solarNoonMs
            val ratio = offsetFromNoon.toFloat() / dayMs.toFloat()
            // Center is 0.5. 12 hours before is 0.0, 12 hours after is 1.0.
            return (0.5f + ratio).coerceIn(-0.2f, 1.2f)
        }

        fun formatInstant(instant: kotlinx.datetime.Instant): String {
            return timeFormatter.format(Date(instant.toEpochMilliseconds()))
        }

        fun getHourMin(instant: kotlinx.datetime.Instant): Pair<Int, Int> {
            val cal = Calendar.getInstance().apply { timeInMillis = instant.toEpochMilliseconds() }
            return cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
        }

        val (fajrHr, fajrMin) = getHourMin(adjustedFajr)
        val (dhuhrHr, dhuhrMin) = getHourMin(adjustedDhuhr)
        val (asrHr, asrMin) = getHourMin(adjustedAsr)
        val (maghribHr, maghribMin) = getHourMin(adjustedMaghrib)
        val (ishaHr, ishaMin) = getHourMin(adjustedIsha)

        val mappedPrayers = listOf(
            PrayerInfo(
                name = "Fajr",
                ar = "الفجر",
                time = formatInstant(adjustedFajr),
                hour = fajrHr,
                minute = fajrMin,
                arcT = tToArc(adjustedFajr.toEpochMilliseconds()),
                dotColorLight = androidx.compose.ui.graphics.Color(0xFF4a70b0),
                dotColorDark = androidx.compose.ui.graphics.Color(0xFF6890d8),
                rawTime = adjustedFajr
            ),
            PrayerInfo(
                name = "Dhuhr",
                ar = "الظهر",
                time = formatInstant(adjustedDhuhr),
                hour = dhuhrHr,
                minute = dhuhrMin,
                arcT = tToArc(adjustedDhuhr.toEpochMilliseconds()),
                dotColorLight = androidx.compose.ui.graphics.Color(0xFFa87010),
                dotColorDark = androidx.compose.ui.graphics.Color(0xFFd4a828),
                rawTime = adjustedDhuhr
            ),
            PrayerInfo(
                name = "Asr",
                ar = "العصر",
                time = formatInstant(adjustedAsr),
                hour = asrHr,
                minute = asrMin,
                arcT = tToArc(adjustedAsr.toEpochMilliseconds()),
                dotColorLight = androidx.compose.ui.graphics.Color(0xFFa06020),
                dotColorDark = androidx.compose.ui.graphics.Color(0xFFd08840),
                rawTime = adjustedAsr
            ),
            PrayerInfo(
                name = "Maghrib",
                ar = "المغرب",
                time = formatInstant(adjustedMaghrib),
                hour = maghribHr,
                minute = maghribMin,
                arcT = tToArc(adjustedMaghrib.toEpochMilliseconds()),
                dotColorLight = androidx.compose.ui.graphics.Color(0xFF9a3828),
                dotColorDark = androidx.compose.ui.graphics.Color(0xFFe06050),
                rawTime = adjustedMaghrib
            ),
            PrayerInfo(
                name = "Isha",
                ar = "العشاء",
                time = formatInstant(adjustedIsha),
                hour = ishaHr,
                minute = ishaMin,
                arcT = tToArc(adjustedIsha.toEpochMilliseconds()),
                dotColorLight = androidx.compose.ui.graphics.Color(0xFF584898),
                dotColorDark = androidx.compose.ui.graphics.Color(0xFF9070d0),
                rawTime = adjustedIsha
            )
        )

        val selectedExtraTimings = settings.selectedExtraPrayerTimings
        val mappedExtraTimings = selectedExtraTimings.mapNotNull { id ->
            val timingDate = extraPrayerTimes[id] ?: return@mapNotNull null
            val timing = kotlinx.datetime.Instant.fromEpochMilliseconds(timingDate.time)
            val (hr, min) = getHourMin(timing)
            PrayerInfo(
                name = extraPrayerTimingShortLabel(id),
                ar = extraPrayerTimingArabicLabel(id),
                time = formatInstant(timing),
                hour = hr,
                minute = min,
                arcT = tToArc(timing.toEpochMilliseconds()),
                dotColorLight = when (id) {
                    "IMSAK" -> androidx.compose.ui.graphics.Color(0xFF5D739C)
                    "SUNRISE" -> androidx.compose.ui.graphics.Color(0xFFCC8B33)
                    "SUNSET" -> androidx.compose.ui.graphics.Color(0xFFAF5A3E)
                    "FIRST_THIRD" -> androidx.compose.ui.graphics.Color(0xFF6F66AF)
                    "MIDNIGHT" -> androidx.compose.ui.graphics.Color(0xFF534E9B)
                    "LAST_THIRD" -> androidx.compose.ui.graphics.Color(0xFF4D79B2)
                    else -> androidx.compose.ui.graphics.Color(0xFF8A8A8A)
                },
                dotColorDark = when (id) {
                    "IMSAK" -> androidx.compose.ui.graphics.Color(0xFF7E95C1)
                    "SUNRISE" -> androidx.compose.ui.graphics.Color(0xFFE7B055)
                    "SUNSET" -> androidx.compose.ui.graphics.Color(0xFFD37858)
                    "FIRST_THIRD" -> androidx.compose.ui.graphics.Color(0xFF8B80D1)
                    "MIDNIGHT" -> androidx.compose.ui.graphics.Color(0xFF7C74D4)
                    "LAST_THIRD" -> androidx.compose.ui.graphics.Color(0xFF70A2E6)
                    else -> androidx.compose.ui.graphics.Color(0xFFB0B0B0)
                },
                rawTime = timing,
                isExtra = true
            )
        }.sortedBy { it.rawTime }

        val min20 = 20 * 60 * 1000L
        val min15 = 15 * 60 * 1000L

        val makruhSunrise = MakruhZone(
            tStart = tToArc(sunriseInstant.toEpochMilliseconds()),
            tEnd = tToArc(sunriseInstant.toEpochMilliseconds() + min20),
            label = "Sunrise",
            description = "Disliked to pray until the sun rises a spear's height (~20 min). \"Whoever catches a rak'ah of Fajr before sunrise has caught Fajr.\" — Bukhari 579, Muslim 608"
        )
        val makruhZawal = MakruhZone(
            tStart = tToArc(adjustedDhuhr.toEpochMilliseconds() - min15),
            tEnd = tToArc(adjustedDhuhr.toEpochMilliseconds()),
            label = "Zawal",
            description = "Sun at zenith — prayer is forbidden at this moment. \"The Prophet ﷺ forbade prayer when the sun is at its peak (Zawal) until it declines.\" — Muslim 831"
        )
        val makruhSunset = MakruhZone(
            tStart = tToArc(adjustedMaghrib.toEpochMilliseconds() - min15),
            tEnd = tToArc(adjustedMaghrib.toEpochMilliseconds()),
            label = "Sunset",
            description = "Disliked to pray as the sun sets. \"Whoever catches a rak'ah of Asr before sunset has caught Asr.\" — Bukhari 556, Muslim 608"
        )

        val todayHijri = runCatching { java.time.chrono.HijrahDate.now() }.getOrNull()
        val formattedHijri = runCatching {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy")
            todayHijri?.format(formatter).orEmpty()
        }.getOrDefault("")

        HomeUiState(
            prayers = mappedPrayers,
            extraTimings = mappedExtraTimings,
            makruhZones = listOf(makruhSunrise, makruhZawal, makruhSunset),

            hijriDate = formattedHijri,
            isRefreshing = isRefreshing,
            usingPreviewTime = usingPreviewTime,
            previewTime = previewTime,
            lastPrayerRefreshEpochMs = settings.lastPrayerRefreshEpochMs,
            locationLat = settings.locationLat,
            locationLng = settings.locationLng,
            locationLabel = settings.locationLabel,
            calculationSource = if (isApiSource) CalculationSource.API else CalculationSource.LOCAL,
            showExtraPrayerTimingsOnHome = settings.showExtraPrayerTimingsOnHome
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )


    fun refreshPrayerData() {
        if (_isRefreshing.value) return
        refreshStartedAtMillis = System.currentTimeMillis()
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                var location: Pair<Float, Float>? = null
                try {
                    val settings = settingsRepository.settingsFlow.first()
                    if (settings.useGpsLocation) {
                        location = withTimeoutOrNull(LOCATION_REFRESH_TIMEOUT_MS) {
                            suspendCancellableCoroutine { continuation ->
                                settingsViewModel.refreshLocation { result ->
                                    if (continuation.isActive) continuation.resume(result)
                                }
                            }
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Continue with the last saved coordinates if GPS lookup fails.
                }
                settingsRepository.updateLocationAndPrayerRefresh(System.currentTimeMillis(), location)
            } finally {
                try {
                    PrayerWidgetProvider.forceWidgetRefresh(appContext)
                } finally {
                    try {
                        waitForMinimumRefreshDuration()
                    } finally {
                        _isRefreshing.value = false
                    }
                }
            }
        }
    }

    fun setPreviewTime(hourOfDay: Int, minute: Int) {
        val cal = Calendar.getInstance().apply {
            time = Date()
            set(Calendar.HOUR_OF_DAY, hourOfDay.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        _previewTimeMillis.value = cal.timeInMillis
    }

    fun clearPreviewTime() {
        _previewTimeMillis.value = null
    }


    companion object {
        private const val MIN_REFRESH_DURATION_MS = 700L
        private const val LOCATION_REFRESH_TIMEOUT_MS = 10_000L

        fun factory(
            settingsRepository: SettingsRepository,
            prayerTimeRepository: PrayerTimeRepository,
            prayerManager: PrayerManager,
            settingsViewModel: SettingsViewModel,
            appContext: Context,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                    return HomeViewModel(
                        settingsRepository,
                        prayerTimeRepository,
                        prayerManager,
                        settingsViewModel,
                        appContext
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
