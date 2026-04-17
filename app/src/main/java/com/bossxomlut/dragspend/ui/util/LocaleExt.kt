package com.bossxomlut.dragspend.ui.util

import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
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
 * Returns a [DatePickerFormatter] whose date/month strings follow
 * the current in-app locale (respects language changes without restart).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberLocalizedDatePickerFormatter(): DatePickerFormatter {
    val appLocale = currentLocale
    return remember(appLocale) {
        object : DatePickerFormatter {
            override fun formatDate(
                dateMillis: Long?,
                locale: Locale,
                forContentDescription: Boolean,
            ): String? {
                if (dateMillis == null) return null
                return SimpleDateFormat("dd MMM yyyy", appLocale).format(Date(dateMillis))
            }

            override fun formatMonthYear(
                monthMillis: Long?,
                locale: Locale,
            ): String? {
                if (monthMillis == null) return null
                return SimpleDateFormat("MMMM yyyy", appLocale).format(Date(monthMillis))
            }
        }
    }
}
