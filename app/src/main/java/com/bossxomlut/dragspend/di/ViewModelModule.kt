package com.bossxomlut.dragspend.di

import com.bossxomlut.dragspend.domain.usecase.card.CreateCardUseCase
import com.bossxomlut.dragspend.domain.usecase.card.DeleteCardUseCase
import com.bossxomlut.dragspend.domain.usecase.card.GetCardsUseCase
import com.bossxomlut.dragspend.domain.usecase.card.IncrementCardUseCountUseCase
import com.bossxomlut.dragspend.domain.usecase.card.UpdateCardUseCase
import com.bossxomlut.dragspend.domain.usecase.category.CreateCategoryUseCase
import com.bossxomlut.dragspend.domain.usecase.category.GetCategoriesUseCase
import com.bossxomlut.dragspend.domain.usecase.profile.DeleteAccountUseCase
import com.bossxomlut.dragspend.domain.usecase.profile.EnsureUserSeededUseCase
import com.bossxomlut.dragspend.domain.usecase.profile.GetProfileUseCase
import com.bossxomlut.dragspend.domain.usecase.profile.UpdateProfileLanguageUseCase
import com.bossxomlut.dragspend.domain.usecase.profile.UpdateProfileNameUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.CopyFromYesterdayUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.CreateTransactionUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.DeleteTransactionUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.GetDailyTransactionsUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.GetMonthlyReportUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.GetMonthlyTransactionsUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.SearchTransactionsUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.UpdateTransactionUseCase
import com.bossxomlut.dragspend.ui.screen.auth.AuthViewModel
import com.bossxomlut.dragspend.ui.screen.dashboard.DashboardViewModel
import com.bossxomlut.dragspend.ui.screen.dashboard.report.CategoryDetailViewModel
import com.bossxomlut.dragspend.ui.screen.dashboard.report.ReportViewModel
import com.bossxomlut.dragspend.ui.screen.dashboard.today.DayDetailViewModel
import com.bossxomlut.dragspend.ui.screen.dashboard.today.TodayViewModel
import com.bossxomlut.dragspend.ui.screen.onboarding.OnboardingViewModel
import com.bossxomlut.dragspend.ui.screen.search.SearchViewModel
import com.bossxomlut.dragspend.ui.screen.settings.SettingsViewModel
import com.bossxomlut.dragspend.util.AppPreferences
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    single { AppPreferences(androidContext()) }

    // Use cases — categories
    single { GetCategoriesUseCase(get(), get()) }
    single { CreateCategoryUseCase(get(), get()) }

    // Use cases — transactions
    single { GetDailyTransactionsUseCase(get(), get()) }
    single { GetMonthlyTransactionsUseCase(get(), get()) }
    single { GetMonthlyReportUseCase(get(), get()) }
    single { CreateTransactionUseCase(get()) }
    single { UpdateTransactionUseCase(get()) }
    single { DeleteTransactionUseCase(get()) }
    single { CopyFromYesterdayUseCase(get(), get()) }
    single { SearchTransactionsUseCase(get(), get()) }

    // Use cases — cards
    single { GetCardsUseCase(get(), get()) }
    single { CreateCardUseCase(get()) }
    single { UpdateCardUseCase(get()) }
    single { DeleteCardUseCase(get()) }
    single { IncrementCardUseCountUseCase(get()) }

    // Use cases — profile
    single { GetProfileUseCase(get(), get(), get()) }
    single { EnsureUserSeededUseCase(get(), get(), get()) }
    single { UpdateProfileNameUseCase(get(), get()) }
    single { UpdateProfileLanguageUseCase(get(), get()) }
    single { DeleteAccountUseCase(get(), get()) }

    // ViewModels
    viewModel { AuthViewModel(get(), get(), get()) }
    viewModel { OnboardingViewModel(get()) }
    viewModel { DashboardViewModel(get(), get(), get()) }
    viewModel {
        TodayViewModel(
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel { DayDetailViewModel(get(), get(), get(), get()) }
    viewModel { CategoryDetailViewModel(get()) }
    viewModel { ReportViewModel(get()) }
    viewModel { SettingsViewModel(androidApplication(), get(), get(), get(), get(), get(), get()) }
    viewModel { SearchViewModel(get(), get()) }
}
