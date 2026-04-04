package com.bossxomlut.dragspend.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bossxomlut.dragspend.domain.usecase.profile.EnsureUserSeededUseCase
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.toFriendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OnboardingUiState {
    data object Idle : OnboardingUiState
    data object Loading : OnboardingUiState
    data class Done(val language: String) : OnboardingUiState
    data class Error(val message: String) : OnboardingUiState
}


class OnboardingViewModel(
    private val ensureUserSeededUseCase: EnsureUserSeededUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun selectLanguage(language: String) {
        AppLog.d(AppLog.Feature.ONBOARDING, "selectLanguage", "language=$language")
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            ensureUserSeededUseCase(name = null, language = language)
                .onSuccess { _uiState.value = OnboardingUiState.Done(language) }
                .onFailure { e -> _uiState.value = OnboardingUiState.Error(e.toFriendlyMessage()) }
        }
    }
}
