package com.bossxomlut.dragspend

import android.app.Application
import android.os.SystemClock
import androidx.appcompat.app.AppCompatDelegate
import com.bossxomlut.dragspend.di.networkModule
import com.bossxomlut.dragspend.di.repositoryModule
import com.bossxomlut.dragspend.di.viewModelModule
import com.bossxomlut.dragspend.util.AppLog
import org.koin.android.ext.koin.androidContext
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
            modules(networkModule, repositoryModule, viewModelModule)
        }

        AppLog.d(AppLog.Feature.PERF, "Application.onCreate", "total=${SystemClock.elapsedRealtime() - t0}ms")
    }
}
