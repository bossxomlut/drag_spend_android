package com.bossxomlut.dragspend.di

import com.bossxomlut.dragspend.data.local.AppDatabase
import com.bossxomlut.dragspend.data.sync.ConnectivityMonitor
import com.bossxomlut.dragspend.data.sync.ConnectivityState
import com.bossxomlut.dragspend.data.sync.SyncLifecycleObserver
import com.bossxomlut.dragspend.data.sync.SyncManager
import com.bossxomlut.dragspend.util.UserIdProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    // Room Database
    single { AppDatabase.getInstance(androidContext()) }

    // DAOs
    single { get<AppDatabase>().transactionDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().spendingCardDao() }
    single { get<AppDatabase>().cardVariantDao() }
    single { get<AppDatabase>().syncQueueDao() }
    single { get<AppDatabase>().syncMetadataDao() }

    // Connectivity
    single { ConnectivityMonitor(androidContext()) }
    single { ConnectivityState(get()) }

    // User ID Provider (supports offline mode)
    single { UserIdProvider(androidContext(), get()) }

    // Sync Manager
    single { SyncManager(get(), get(), get()) }

    // Lifecycle Observer for foreground sync
    factory { SyncLifecycleObserver(get()) }
}
