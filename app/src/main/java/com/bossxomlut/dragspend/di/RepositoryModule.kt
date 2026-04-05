package com.bossxomlut.dragspend.di

import androidx.room.Room
import com.bossxomlut.dragspend.data.local.AppDatabase
import com.bossxomlut.dragspend.data.local.BackupManager
import com.bossxomlut.dragspend.data.local.LocalSeeder
import com.bossxomlut.dragspend.data.local.SyncManager
import com.bossxomlut.dragspend.data.repository.CardRepositoryImpl
import com.bossxomlut.dragspend.data.repository.CategoryRepositoryImpl
import com.bossxomlut.dragspend.data.repository.ProfileRepositoryImpl
import com.bossxomlut.dragspend.data.repository.SessionRepositoryImpl
import com.bossxomlut.dragspend.data.repository.TransactionRepositoryImpl
import com.bossxomlut.dragspend.domain.repository.CardRepository
import com.bossxomlut.dragspend.domain.repository.CategoryRepository
import com.bossxomlut.dragspend.domain.repository.ProfileRepository
import com.bossxomlut.dragspend.domain.repository.SessionRepository
import com.bossxomlut.dragspend.domain.repository.TransactionRepository
import com.bossxomlut.dragspend.util.GuestSession
import com.bossxomlut.dragspend.util.ProfileCache
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    // ── Local DB ─────────────────────────────────────────────────────────────
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "drag_spend_db",
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()
    }
    single { get<AppDatabase>().transactionDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().cardDao() }
    single { get<AppDatabase>().cardVariantDao() }

    // ── Utilities ─────────────────────────────────────────────────────────────
    single { ProfileCache(androidContext()) }
    single { GuestSession(androidContext()) }

    // ── Sync ─────────────────────────────────────────────────────────────────
    single { SyncManager(get(), get(), get(), get(), get()) }
    single { BackupManager(get(), get(), get(), get(), get()) }
    single { LocalSeeder(get(), get(), get()) }

    // ── Repositories ─────────────────────────────────────────────────────────
    single<SessionRepository> { SessionRepositoryImpl(get(), get(), get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get(), get(), get()) }
    single<CardRepository> { CardRepositoryImpl(get(), get(), get(), get(), get()) }
    single<TransactionRepository> { TransactionRepositoryImpl(get(), get(), get(), get(), get()) }
}
