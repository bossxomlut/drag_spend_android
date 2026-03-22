package com.bossxomlut.dragspend

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.bossxomlut.dragspend.navigation.AppNavGraph
import com.bossxomlut.dragspend.receiver.ReminderReceiver
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme
import com.bossxomlut.dragspend.util.AppPreferences
import com.bossxomlut.dragspend.util.ThemeMode
import io.github.jan.supabase.SupabaseClient
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val supabase: SupabaseClient by inject()
    private val appPreferences: AppPreferences by inject()

    private val showSecureOverlay = mutableStateOf(false)

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        showSecureOverlay.value = !hasFocus
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        enableEdgeToEdge()
        setContent {
            val themeMode by appPreferences.themeMode.collectAsStateWithLifecycle(
                initialValue = ThemeMode.SYSTEM,
            )
            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !isDark
                        isAppearanceLightNavigationBars = !isDark
                    }
                }
            }

            DragSpendTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AppNavGraph(
                        navController = navController,
                        supabase = supabase,
                    )
                }

                if (showSecureOverlay.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(R.drawable.ic_app_logo),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(20.dp)),
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderReceiver.CHANNEL_ID,
                getString(R.string.notification_reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = getString(R.string.notification_reminder_channel_desc)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
