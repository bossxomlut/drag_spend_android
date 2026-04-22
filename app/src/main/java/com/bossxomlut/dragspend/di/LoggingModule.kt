package com.bossxomlut.dragspend.di

import com.bossxomlut.dragspend.util.AppTracker
import com.bossxomlut.dragspend.util.reporter.AnalyticsReporter
import com.bossxomlut.dragspend.util.reporter.CrashlyticsReporter
import com.bossxomlut.dragspend.util.reporter.LogcatReporter
import com.google.firebase.analytics.FirebaseAnalytics
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val loggingModule = module {
    single { LogcatReporter() }
    single { CrashlyticsReporter() }
    single { AnalyticsReporter(FirebaseAnalytics.getInstance(androidContext())) }

    // Bind AppTracker interface to AnalyticsReporter — ViewModels inject AppTracker,
    // not AnalyticsReporter, keeping the presentation layer Firebase-agnostic.
    single<AppTracker> { get<AnalyticsReporter>() }
}
