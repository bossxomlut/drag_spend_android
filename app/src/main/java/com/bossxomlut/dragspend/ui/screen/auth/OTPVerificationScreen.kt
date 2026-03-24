package com.bossxomlut.dragspend.ui.screen.auth

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bossxomlut.dragspend.R
import com.bossxomlut.dragspend.ui.components.AppToast
import com.bossxomlut.dragspend.ui.components.ToastType
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

private const val OTP_LENGTH = 6
private const val OTP_EXPIRY_SECONDS = 300 // 5 minutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTPVerificationScreen(
    email: String,
    source: String,
    onNavigateBack: () -> Unit,
    onVerified: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.OTPVerified -> {
                viewModel.resetState()
                onVerified()
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
                    title = {
                        Text(
                            text = stringResource(R.string.otp_verification_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = onNavigateBack) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { innerPadding ->
            OTPVerificationContent(
                modifier = Modifier.padding(innerPadding),
                email = email,
                uiState = uiState,
                onVerifyOTP = { otp -> viewModel.verifyOTP(email, otp) },
                onResendOTP = { viewModel.sendOTP(email) },
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
private fun OTPVerificationContent(
    email: String,
    uiState: AuthUiState,
    onVerifyOTP: (String) -> Unit,
    onResendOTP: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var otp by remember { mutableStateOf("") }
    var secondsRemaining by remember { mutableIntStateOf(OTP_EXPIRY_SECONDS) }
    var isExpired by remember { mutableStateOf(false) }
    val isLoading = uiState is AuthUiState.Loading
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Countdown timer
    LaunchedEffect(key1 = Unit) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
        }
        isExpired = true
    }

    // Reset timer when resend is clicked
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.OTPSent) {
            secondsRemaining = OTP_EXPIRY_SECONDS
            isExpired = false
            otp = ""
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

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

        // Email icon in circle
        Surface(
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.otp_verification_heading),
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
            text = buildAnnotatedString {
                append(stringResource(R.string.otp_verification_description))
                append(" ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                    append(email)
                }
            },
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
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // OTP Input Boxes
                OTPInputField(
                    otp = otp,
                    onOtpChange = { newOtp ->
                        if (newOtp.length <= OTP_LENGTH && newOtp.all { it.isDigit() }) {
                            otp = newOtp
                        }
                    },
                    isExpired = isExpired,
                    focusRequester = focusRequester,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Timer
                TimerDisplay(
                    secondsRemaining = secondsRemaining,
                    isExpired = isExpired,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Verify button
                Button(
                    onClick = { onVerifyOTP(otp) },
                    enabled = !isLoading && otp.length == OTP_LENGTH && !isExpired,
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
                            text = stringResource(R.string.action_verify_otp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Resend button
                TextButton(
                    onClick = {
                        onResendOTP()
                    },
                    enabled = !isLoading,
                ) {
                    Text(
                        text = stringResource(R.string.action_resend_otp),
                        color = if (isLoading) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun OTPInputField(
    otp: String,
    onOtpChange: (String) -> Unit,
    isExpired: Boolean,
    focusRequester: FocusRequester,
) {
    BasicTextField(
        value = otp,
        onValueChange = onOtpChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.focusRequester(focusRequester),
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                repeat(OTP_LENGTH) { index ->
                    val char = otp.getOrNull(index)?.toString() ?: ""
                    val isFocused = otp.length == index

                    OTPBox(
                        char = char,
                        isFocused = isFocused,
                        isExpired = isExpired,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    )
}

@Composable
private fun OTPBox(
    char: String,
    isFocused: Boolean,
    isExpired: Boolean,
    modifier: Modifier = Modifier,
) {
    val borderColor = when {
        isExpired -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        isFocused -> MaterialTheme.colorScheme.primary
        char.isNotEmpty() -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outline
    }

    val backgroundColor = when {
        isExpired -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        char.isNotEmpty() -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .height(56.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = char,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (isExpired) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun TimerDisplay(
    secondsRemaining: Int,
    isExpired: Boolean,
) {
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timerText = String.format("%d:%02d", minutes, seconds)

    val color = when {
        isExpired -> MaterialTheme.colorScheme.error
        secondsRemaining <= 60 -> Color(0xFFFF9800) // Orange warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (isExpired) {
                stringResource(R.string.otp_expired)
            } else {
                timerText
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            ),
            color = color,
        )
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark")
@Composable
private fun OTPVerificationScreenPreview() {
    DragSpendTheme {
        OTPVerificationContent(
            email = "test@example.com",
            uiState = AuthUiState.Idle,
            onVerifyOTP = {},
            onResendOTP = {},
        )
    }
}
