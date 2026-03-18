package com.bossxomlut.dragspend.di

import android.content.Context
import com.bossxomlut.dragspend.BuildConfig
import com.bossxomlut.dragspend.util.SharedPreferencesSessionManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.android.Android
import org.koin.dsl.module

val networkModule = module {
    single<SupabaseClient> {
        val context = get<Context>()
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth) {
                sessionManager = SharedPreferencesSessionManager(context)
            }
            install(Postgrest)
            httpEngine = Android.create()
        }
    }
}
