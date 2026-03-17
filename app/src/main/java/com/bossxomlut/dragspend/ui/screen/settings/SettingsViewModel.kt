package com.bossxomlut.dragspend.ui.screen.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bossxomlut.dragspend.data.model.Profile
import com.bossxomlut.dragspend.domain.repository.ProfileRepository
import com.bossxomlut.dragspend.util.AppPreferences
import com.bossxomlut.dragspend.util.ReminderScheduler
import com.bossxomlut.dragspend.util.ThemeMode
import com.bossxomlut.dragspend.util.toFriendlyMessage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val displayName: String = "",
    val email: String = "",
    val language: String = "vi",
    val isLoading: Boolean = false,
    val error: String? = null,
    val signedOut: Boolean = false,
    val accountDeleted: Boolean = false,
)

class SettingsViewModel(
    application: Application,
    private val supabase: SupabaseClient,
    private val profileRepository: ProfileRepository,
    private val appPreferences: AppPreferences,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = appPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val reminderEnabled: StateFlow<Boolean> = appPreferences.reminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val reminderHour: StateFlow<Int> = appPreferences.reminderHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 20)

    val reminderMinute: StateFlow<Int> = appPreferences.reminderMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val user = supabase.auth.currentUserOrNull() ?: return
        _uiState.update { it.copy(email = user.email ?: "", isLoading = true) }
        viewModelScope.launch {
            profileRepository.getProfile(user.id)
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            displayName = profile.name ?: "",
                            language = profile.language ?: "vi",
                            isLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.toFriendlyMessage()) }
                }
        }
    }

    fun updateName(name: String) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            profileRepository.updateName(userId, name.trim())
                .onSuccess { _uiState.update { it.copy(displayName = name.trim()) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.toFriendlyMessage()) } }
        }
    }

    fun setLanguage(language: String) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        _uiState.update { it.copy(language = language) }
        viewModelScope.launch {
            profileRepository.updateLanguage(userId, language)
                .onFailure { e -> _uiState.update { it.copy(error = e.toFriendlyMessage()) } }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { appPreferences.setThemeMode(mode) }
    }

    fun setReminderEnabled(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            appPreferences.setReminderEnabled(enabled)
            if (enabled) {
                ReminderScheduler.schedule(getApplication(), hour, minute)
            } else {
                ReminderScheduler.cancel(getApplication())
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            appPreferences.setReminderTime(hour, minute)
            if (reminderEnabled.value) {
                ReminderScheduler.schedule(getApplication(), hour, minute)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { supabase.auth.signOut() }
                .onSuccess { _uiState.update { it.copy(signedOut = true) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.toFriendlyMessage()) } }
        }
    }

    fun deleteAccount() {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            profileRepository.softDeleteAccount(userId)
                .onSuccess {
                    runCatching { supabase.auth.signOut() }
                    _uiState.update { it.copy(accountDeleted = true) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.toFriendlyMessage()) } }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
