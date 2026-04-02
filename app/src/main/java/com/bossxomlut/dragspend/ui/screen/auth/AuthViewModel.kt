package com.bossxomlut.dragspend.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bossxomlut.dragspend.domain.repository.SessionRepository
import com.bossxomlut.dragspend.domain.usecase.profile.GetProfileUseCase
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.toFriendlyMessage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    /** After registration: email and OTP sent to confirm account. */
    data class EmailConfirmationPending(val email: String) : AuthUiState
    /** After login attempt when email is not yet confirmed. */
    data class EmailNotConfirmed(val email: String) : AuthUiState
    /** Login successful — carries whether the user still needs onboarding. */
    data class LoginSuccess(val needsOnboarding: Boolean) : AuthUiState
    data object OTPSent : AuthUiState
    data object OTPVerified : AuthUiState
    data object PasswordResetSuccess : AuthUiState
    data class Error(val message: String) : AuthUiState
}


class AuthViewModel(
    private val supabase: SupabaseClient,
    private val sessionRepository: SessionRepository,
    private val getProfileUseCase: GetProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        AppLog.d(AppLog.Feature.AUTH, "login", "email=$email")
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            }.onSuccess {
                AppLog.success(AppLog.Feature.AUTH, "login")
                if (!sessionRepository.isAuthenticated()) {
                    _uiState.value = AuthUiState.LoginSuccess(needsOnboarding = false)
                    return@onSuccess
                }
                val profile = getProfileUseCase().getOrNull()
                val needsOnboarding = profile?.language == null
                AppLog.d(AppLog.Feature.AUTH, "login", "needsOnboarding=$needsOnboarding")
                _uiState.value = AuthUiState.LoginSuccess(needsOnboarding = needsOnboarding)
            }.onFailure { e ->
                AppLog.error(AppLog.Feature.AUTH, "login", e)
                val raw = e.message?.lowercase() ?: ""
                if (raw.contains("email not confirmed") || raw.contains("email_not_confirmed")) {
                    // Email not yet confirmed — send a 6-digit OTP so the user can verify now
                    runCatching {
                        supabase.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.OTP) {
                            this.email = email
                            createUser = false
                        }
                    }.onSuccess {
                        AppLog.success(AppLog.Feature.AUTH, "login", "OTP sent for unconfirmed email")
                        _uiState.value = AuthUiState.EmailNotConfirmed(email)
                    }.onFailure { e2 ->
                        AppLog.error(AppLog.Feature.AUTH, "login", e2)
                        _uiState.value = AuthUiState.Error(e2.toFriendlyMessage())
                    }
                } else {
                    _uiState.value = AuthUiState.Error(e.toFriendlyMessage())
                }
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        AppLog.d(AppLog.Feature.AUTH, "register", "email=$email, name=$name")
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching {
                // Step 1: create the account
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    data = buildJsonObject {
                        put("name", name.trim())
                    }
                }
                // Step 2: explicitly send a 6-digit OTP via sign-in OTP flow.
                // signUpWith sends a link-based confirmation email by default;
                // signInWith(OTP) always sends the 6-digit numeric code.
                supabase.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.OTP) {
                    this.email = email
                    createUser = false
                }
            }.onSuccess {
                AppLog.success(AppLog.Feature.AUTH, "register", "OTP sent to $email")
                _uiState.value = AuthUiState.EmailConfirmationPending(email)
            }.onFailure { e ->
                AppLog.error(AppLog.Feature.AUTH, "register", e)
                _uiState.value = AuthUiState.Error(e.toFriendlyMessage())
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        AppLog.d(AppLog.Feature.AUTH, "sendPasswordResetEmail", "email=$email")
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching {
                supabase.auth.resetPasswordForEmail(email, redirectUrl = "dragspend://auth/reset-password")
            }.onSuccess {
                AppLog.success(AppLog.Feature.AUTH, "sendPasswordResetEmail")
                _uiState.value = AuthUiState.Success
            }.onFailure { e ->
                AppLog.error(AppLog.Feature.AUTH, "sendPasswordResetEmail", e)
                _uiState.value = AuthUiState.Error(e.toFriendlyMessage())
            }
        }
    }

    /**
     * Send OTP to email for password reset flow.
     * Uses signInWithOTP which sends a 6-digit code to the user's email.
     */
    fun sendOTP(email: String) {
        AppLog.d(AppLog.Feature.AUTH, "sendOTP", "email=$email")
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching {
                supabase.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.OTP) {
                    this.email = email
                    createUser = false
                }
            }.onSuccess {
                AppLog.success(AppLog.Feature.AUTH, "sendOTP")
                _uiState.value = AuthUiState.OTPSent
            }.onFailure { e ->
                AppLog.error(AppLog.Feature.AUTH, "sendOTP", e)
                _uiState.value = AuthUiState.Error(e.toFriendlyMessage())
            }
        }
    }

    /**
     * Verify OTP code entered by user.
     * If successful, creates a session that allows password update.
     */
    fun verifyOTP(email: String, token: String) {
        AppLog.d(AppLog.Feature.AUTH, "verifyOTP", "email=$email")
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching {
                supabase.auth.verifyEmailOtp(
                    type = OtpType.Email.EMAIL,
                    email = email,
                    token = token,
                )
            }.onSuccess {
                AppLog.success(AppLog.Feature.AUTH, "verifyOTP")
                _uiState.value = AuthUiState.OTPVerified
            }.onFailure { e ->
                AppLog.error(AppLog.Feature.AUTH, "verifyOTP", e)
                _uiState.value = AuthUiState.Error(e.toFriendlyMessage())
            }
        }
    }

    /**
     * Update the user's password after OTP verification.
     * Requires an active session from verifyOTP.
     */
    fun updatePassword(newPassword: String) {
        AppLog.d(AppLog.Feature.AUTH, "updatePassword", "updating password")
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching {
                supabase.auth.updateUser {
                    password = newPassword
                }
            }.onSuccess {
                AppLog.success(AppLog.Feature.AUTH, "updatePassword")
                // Sign out after password reset so user can login with new password
                runCatching { supabase.auth.signOut() }
                _uiState.value = AuthUiState.PasswordResetSuccess
            }.onFailure { e ->
                AppLog.error(AppLog.Feature.AUTH, "updatePassword", e)
                _uiState.value = AuthUiState.Error(e.toFriendlyMessage())
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
