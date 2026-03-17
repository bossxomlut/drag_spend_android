package com.bossxomlut.dragspend.di

import com.bossxomlut.dragspend.data.repository.CardRepositoryImpl
import com.bossxomlut.dragspend.data.repository.CategoryRepositoryImpl
import com.bossxomlut.dragspend.data.repository.ProfileRepositoryImpl
import com.bossxomlut.dragspend.data.repository.TransactionRepositoryImpl
import com.bossxomlut.dragspend.domain.repository.CardRepository
import com.bossxomlut.dragspend.domain.repository.CategoryRepository
import com.bossxomlut.dragspend.domain.repository.ProfileRepository
import com.bossxomlut.dragspend.domain.repository.TransactionRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get()) }
    single<CardRepository> { CardRepositoryImpl(get()) }
    single<TransactionRepository> { TransactionRepositoryImpl(get()) }
}
