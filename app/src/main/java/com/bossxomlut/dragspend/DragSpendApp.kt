package com.bossxomlut.dragspend

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ProcessLifecycleOwner
import com.bossxomlut.dragspend.data.sync.SyncLifecycleObserver
import com.bossxomlut.dragspend.data.sync.SyncManager
import com.bossxomlut.dragspend.di.databaseModule
import com.bossxomlut.dragspend.di.networkModule
import com.bossxomlut.dragspend.di.repositoryModule
import com.bossxomlut.dragspend.di.viewModelModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DragSpendApp : Application() {

    private val syncManager: SyncManager by inject()
    private val syncLifecycleObserver: SyncLifecycleObserver by inject()

    override fun onCreate() {
        super.onCreate()

        // Apply saved night mode before any activity is created
        val prefs = getSharedPreferences("night_mode_prefs", MODE_PRIVATE)
        val nightMode = prefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(nightMode)

        startKoin {
            androidContext(this@DragSpendApp)
            modules(networkModule, databaseModule, repositoryModule, viewModelModule)
        }

        // Start observing connectivity changes for sync
        syncManager.startObserving()

        // Register lifecycle observer for foreground sync
        ProcessLifecycleOwner.get().lifecycle.addObserver(syncLifecycleObserver)
    }
}
