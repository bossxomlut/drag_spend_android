package com.bossxomlut.dragspend.util

import android.content.Context
import androidx.core.content.edit

/**
 * Lightweight SharedPreferences cache for the profile's language field.
 *
 * Only storing `language` is enough to short-circuit the startup network call:
 * - null → user hasn't completed onboarding (or account deleted) → always fetch fresh
 * - non-null → user is a normal active user → go to Dashboard immediately
 */
class ProfileCache(context: Context) {

    private val prefs = context.getSharedPreferences("profile_cache_v1", Context.MODE_PRIVATE)

    /** Returns the cached language for [userId], or null if not in cache. */
    fun getLanguage(userId: String): String? = prefs.getString("${userId}_lang", null)

    /** Persist [language] for [userId] after a successful profile fetch with onboarding done. */
    fun saveLanguage(userId: String, language: String) {
        prefs.edit { putString("${userId}_lang", language) }
    }

    /** Clear all cached data — call on sign-out so the next login re-fetches a fresh profile. */
    fun clear() {
        prefs.edit { clear() }
    }
}
