package com.bossxomlut.dragspend

import android.app.Application
import android.os.SystemClock
import androidx.appcompat.app.AppCompatDelegate
import com.bossxomlut.dragspend.di.loggingModule
import com.bossxomlut.dragspend.di.networkModule
import com.bossxomlut.dragspend.di.repositoryModule
import com.bossxomlut.dragspend.di.viewModelModule
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.reporter.AnalyticsReporter
import com.bossxomlut.dragspend.util.reporter.CrashlyticsReporter
import com.bossxomlut.dragspend.util.reporter.LogcatReporter
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class DragSpendApp : Application() {

    override fun onCreate() {
        val t0 = SystemClock.elapsedRealtime()
        super.onCreate()

        // Apply saved night mode before any activity is created
        val prefs = getSharedPreferences("night_mode_prefs", MODE_PRIVATE)
        val nightMode = prefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(nightMode)

        startKoin {
            androidContext(this@DragSpendApp)
            modules(loggingModule, networkModule, repositoryModule, viewModelModule)
        }

        // Install reporters into AppLog after Koin is ready.
        // loggingModule must be listed first above so these singletons are already created.
        val koin = GlobalContext.get()
        AppLog.install(
            koin.get<LogcatReporter>(),
            koin.get<CrashlyticsReporter>(),
            koin.get<AnalyticsReporter>(),
        )

        val crashlyticsReporter = koin.get<CrashlyticsReporter>()
        if (crashlyticsReporter.isAvailable) {
            AppLog.d(AppLog.Feature.PERF, "Crashlytics", "initialized successfully")
        } else {
            AppLog.w(AppLog.Feature.PERF, "Crashlytics", "component not available — google-services.json missing or invalid")
        }

        AppLog.d(AppLog.Feature.PERF, "Application.onCreate", "total=${SystemClock.elapsedRealtime() - t0}ms")
    }
}
