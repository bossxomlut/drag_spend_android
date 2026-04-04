package com.bossxomlut.dragspend.di

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
import com.bossxomlut.dragspend.util.ProfileCache
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    single { ProfileCache(androidContext()) }
    single<SessionRepository> { SessionRepositoryImpl(get(), get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get()) }
    single<CardRepository> { CardRepositoryImpl(get()) }
    single<TransactionRepository> { TransactionRepositoryImpl(get(), get()) }
}
