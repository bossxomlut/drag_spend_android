package com.bossxomlut.dragspend.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class AppPreferences(private val context: Context) {

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        private val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val KEY_REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val reminderEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_REMINDER_ENABLED] ?: false
    }

    val reminderHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_REMINDER_HOUR] ?: 20
    }

    val reminderMinute: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_REMINDER_MINUTE] ?: 0
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = mode.name }
        // Update system night mode for splash screen on next launch
        val nightMode = when (mode) {
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
        // Also save to SharedPreferences for Application startup
        context.getSharedPreferences("night_mode_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("night_mode", nightMode)
            .apply()
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REMINDER_HOUR] = hour
            prefs[KEY_REMINDER_MINUTE] = minute
        }
    }
}
