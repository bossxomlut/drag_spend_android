package com.bossxomlut.dragspend.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

/**
 * Returns the current [Locale] derived from the app's in-process
 * [LocalContext], which respects the locale override injected via
 * [createConfigurationContext] in AppNavGraph.
 *
 * Prefer this over [Locale.getDefault], which reads the JVM system locale
 * and does NOT reflect in-app language changes.
 */
val currentLocale: Locale
    @Composable
    @ReadOnlyComposable
    get() = LocalContext.current.resources.configuration.locales.get(0)

/**
 * Short display name for [DayOfWeek] that is locale-aware.
 * Vietnamese returns standard abbreviations "T2"–"T7", "CN" instead of
 * the ICU single-letter form that Java returns for Locale("vi").
 */
fun DayOfWeek.localizedShortName(locale: Locale): String {
    if (locale.language == "vi") {
        return when (this) {
            DayOfWeek.MONDAY -> "T2"
            DayOfWeek.TUESDAY -> "T3"
            DayOfWeek.WEDNESDAY -> "T4"
            DayOfWeek.THURSDAY -> "T5"
            DayOfWeek.FRIDAY -> "T6"
            DayOfWeek.SATURDAY -> "T7"
            DayOfWeek.SUNDAY -> "CN"
        }
    }
    return getDisplayName(TextStyle.SHORT, locale)
}

/**
 * Formats a month+year string that is locale-aware.
 * Vietnamese capitalises "Tháng" and inserts a comma: "Tháng 4, 2026".
 */
fun formatMonthYear(monthMillis: Long, locale: Locale): String {
    val date = Date(monthMillis)
    return if (locale.language == "vi") {
        val month = SimpleDateFormat("M", locale).format(date)
        val year = SimpleDateFormat("yyyy", locale).format(date)
        "Tháng $month, $year"
    } else {
        SimpleDateFormat("MMMM yyyy", locale).format(date)
            .replaceFirstChar { it.uppercase(locale) }
    }
}
