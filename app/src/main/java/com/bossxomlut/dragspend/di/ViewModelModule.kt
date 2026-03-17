package com.bossxomlut.dragspend.di

import com.bossxomlut.dragspend.ui.screen.auth.AuthViewModel
import com.bossxomlut.dragspend.ui.screen.dashboard.DashboardViewModel
import com.bossxomlut.dragspend.ui.screen.dashboard.report.ReportViewModel
import com.bossxomlut.dragspend.ui.screen.dashboard.today.TodayViewModel
import com.bossxomlut.dragspend.ui.screen.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { AuthViewModel(get(), get()) }
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { DashboardViewModel(get(), get()) }
    viewModel { TodayViewModel(get(), get(), get()) }
    viewModel { ReportViewModel(get(), get()) }
}
