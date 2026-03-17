package com.bossxomlut.dragspend.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bossxomlut.dragspend.domain.repository.ProfileRepository
import com.bossxomlut.dragspend.util.toFriendlyMessage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data object EmailConfirmationPending : AuthUiState
    data class Error(val message: String) : AuthUiState
}


class AuthViewModel(
    private val supabase: SupabaseClient,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            }.onSuccess {
                _uiState.value = AuthUiState.Success
            }.onFailure { e ->
                _uiState.value = AuthUiState.Error(e.toFriendlyMessage())
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    data = buildJsonObject {
                        put("name", name.trim())
                    }
                }
            }.onSuccess {
                _uiState.value = AuthUiState.EmailConfirmationPending
            }.onFailure { e ->
                _uiState.value = AuthUiState.Error(e.toFriendlyMessage())
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching {
                supabase.auth.resetPasswordForEmail(email, redirectUrl = "dragspend://auth/reset-password")
            }.onSuccess {
                _uiState.value = AuthUiState.Success
            }.onFailure { e ->
                _uiState.value = AuthUiState.Error(e.toFriendlyMessage())
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
