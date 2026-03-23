package com.bossxomlut.dragspend.data.sync

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bossxomlut.dragspend.util.AppLog

/**
 * Lifecycle observer that triggers sync when the app comes to foreground.
 */
class SyncLifecycleObserver(
    private val syncManager: SyncManager,
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        AppLog.d(AppLog.Feature.SYNC, "SyncLifecycleObserver", "App started, triggering foreground sync")
        syncManager.onAppForeground()
    }
}
