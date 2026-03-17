package com.bossxomlut.dragspend

import android.app.Application
import com.bossxomlut.dragspend.di.networkModule
import com.bossxomlut.dragspend.di.repositoryModule
import com.bossxomlut.dragspend.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DragSpendApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@DragSpendApp)
            modules(networkModule, repositoryModule, viewModelModule)
        }
    }
}
