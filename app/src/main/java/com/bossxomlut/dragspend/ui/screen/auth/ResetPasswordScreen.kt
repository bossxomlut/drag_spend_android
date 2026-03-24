package com.bossxomlut.dragspend.ui.screen.auth

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bossxomlut.dragspend.R
import com.bossxomlut.dragspend.ui.components.AppToast
import com.bossxomlut.dragspend.ui.components.ToastType
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Error -> {
                toastMessage = state.message
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.reset_password_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { innerPadding ->
            ResetPasswordContent(
                modifier = Modifier.padding(innerPadding),
                uiState = uiState,
                onResetPassword = viewModel::updatePassword,
                onNavigateToLogin = onNavigateToLogin,
            )
        }
        AppToast(
            message = toastMessage,
            type = ToastType.ERROR,
            onDismiss = { toastMessage = null },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp),
        )
    }
}

@Composable
private fun ResetPasswordContent(
    uiState: AuthUiState,
    onResetPassword: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val isLoading = uiState is AuthUiState.Loading
    val isSuccess = uiState is AuthUiState.PasswordResetSuccess
    val focusManager = LocalFocusManager.current

    val passwordsMatchError = stringResource(R.string.error_passwords_do_not_match)
    val passwordTooShortError = stringResource(R.string.error_password_too_short)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Shield icon in circle
        Surface(
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isSuccess) {
            // Success State
            Text(
                text = stringResource(R.string.reset_password_success_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.reset_password_success_message),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.action_back_to_login),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else {
            // Form State
            Text(
                text = stringResource(R.string.reset_password_heading),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.reset_password_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.label_new_password),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        placeholder = { Text(stringResource(R.string.password_placeholder)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.label_confirm_password),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            passwordError = null
                        },
                        placeholder = { Text(stringResource(R.string.confirm_password_placeholder)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (password.isNotBlank() && confirmPassword.isNotBlank()) {
                                    when {
                                        password.length < 6 -> passwordError = passwordTooShortError
                                        password != confirmPassword -> passwordError = passwordsMatchError
                                        else -> onResetPassword(password)
                                    }
                                }
                            },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        isError = passwordError != null,
                    )

                    if (passwordError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = passwordError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            when {
                                password.length < 6 -> passwordError = passwordTooShortError
                                password != confirmPassword -> passwordError = passwordsMatchError
                                else -> onResetPassword(password)
                            }
                        },
                        enabled = !isLoading && password.isNotBlank() && confirmPassword.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.action_save_password),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark")
@Composable
private fun ResetPasswordScreenPreview() {
    DragSpendTheme {
        ResetPasswordContent(
            uiState = AuthUiState.Idle,
            onResetPassword = {},
            onNavigateToLogin = {},
        )
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Success Light")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Success Dark")
@Composable
private fun ResetPasswordSuccessPreview() {
    DragSpendTheme {
        ResetPasswordContent(
            uiState = AuthUiState.PasswordResetSuccess,
            onResetPassword = {},
            onNavigateToLogin = {},
        )
    }
}
