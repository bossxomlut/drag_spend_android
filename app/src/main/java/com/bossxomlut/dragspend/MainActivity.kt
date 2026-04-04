package com.bossxomlut.dragspend

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.bossxomlut.dragspend.navigation.AppNavGraph
import com.bossxomlut.dragspend.receiver.ReminderReceiver
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.AppPreferences
import com.bossxomlut.dragspend.util.ThemeMode
import io.github.jan.supabase.SupabaseClient
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val supabase: SupabaseClient by inject()
    private val appPreferences: AppPreferences by inject()

    private val isAppReady = mutableStateOf(false)
    private val activityStartTime = SystemClock.elapsedRealtime()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        AppLog.d(AppLog.Feature.PERF, "MainActivity.onCreate", "started")

        applySecureFlag("onCreate")

        // Keep splash screen visible until app is ready
        splashScreen.setKeepOnScreenCondition { !isAppReady.value }

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
                        onReady = {
                            val elapsed = SystemClock.elapsedRealtime() - activityStartTime
                            AppLog.success(AppLog.Feature.PERF, "splash_dismissed", "${elapsed}ms since Activity start")
                            isAppReady.value = true
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applySecureFlag("onResume")
    }

    private fun applySecureFlag(source: String) {
        // Always secure this window: prevents screenshots & recents preview.
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        val isSecure =
            (window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE) != 0
        AppLog.d(
            AppLog.Feature.APP,
            "secureOverlay",
            "$source -> FLAG_SECURE applied, isSecure=$isSecure",
        )
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
