package com.bossxomlut.dragspend.ui.screen.auth

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bossxomlut.dragspend.R
import com.bossxomlut.dragspend.ui.components.AppToast
import com.bossxomlut.dragspend.ui.components.ToastType
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToOTP: (email: String) -> Unit,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.EmailConfirmationPending -> {
                viewModel.resetState()
                onNavigateToOTP(state.email)
            }
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
                    title = {},
                    navigationIcon = {
                        TextButton(onClick = onNavigateToLogin) {
                            Text(stringResource(R.string.action_close))
                        }
                    },
                    actions = {
                        RegisterLanguageSwitcher(
                            selectedLanguage = selectedLanguage,
                            onLanguageSelected = onLanguageSelected,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { innerPadding ->
            RegisterContent(
                modifier = Modifier.padding(innerPadding),
                uiState = uiState,
                onRegister = viewModel::register,
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
private fun RegisterContent(
    uiState: AuthUiState,
    onRegister: (name: String, email: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val isLoading = uiState is AuthUiState.Loading
    val isPasswordMismatch = confirmPassword.isNotEmpty() && confirmPassword != password
    val canSubmit = !isLoading && name.isNotBlank() && email.isNotBlank() &&
            password.isNotBlank() && confirmPassword == password

    val focusManager = LocalFocusManager.current
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp)),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.register_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Name
                    Text(
                        text = stringResource(R.string.label_your_name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("John Doe") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { emailFocusRequester.requestFocus() },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email
                    Text(
                        text = stringResource(R.string.label_email),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("you@example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { passwordFocusRequester.requestFocus() },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(emailFocusRequester),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password
                    Text(
                        text = stringResource(R.string.label_password),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text(stringResource(R.string.password_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { confirmPasswordFocusRequester.requestFocus() },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm password
                    Text(
                        text = stringResource(R.string.label_confirm_password),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isPasswordMismatch) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = { Text(stringResource(R.string.confirm_password_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        isError = isPasswordMismatch,
                        supportingText = if (isPasswordMismatch) {
                            { Text(stringResource(R.string.error_passwords_do_not_match)) }
                        } else {
                            null
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (canSubmit) onRegister(name, email, password)
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(confirmPasswordFocusRequester),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onRegister(name, email, password) },
                        enabled = canSubmit,
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
                                text = stringResource(R.string.action_sign_up),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RegisterLanguageSwitcher(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RegisterLanguageTab(
                flag = "🇻🇳",
                code = "VI",
                isSelected = selectedLanguage == "vi",
                onClick = { onLanguageSelected("vi") },
            )
            RegisterLanguageTab(
                flag = "🇺🇸",
                code = "EN",
                isSelected = selectedLanguage == "en",
                onClick = { onLanguageSelected("en") },
            )
        }
    }
}

@Composable
private fun RegisterLanguageTab(
    flag: String,
    code: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = flag,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark")
@Composable
private fun RegisterScreenPreview() {
    DragSpendTheme {
        RegisterContent(
            uiState = AuthUiState.Idle,
            onRegister = { _, _, _ -> },
            onNavigateToLogin = {},
        )
    }
}

