package com.bossxomlut.dragspend.util

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

/**
 * Helper to get the current user ID, with fallback to stored session for offline mode.
 *
 * In online mode: Returns the current Supabase auth user ID
 * In offline mode: Returns the stored user ID from the last successful session
 */
class UserIdProvider(
    private val context: Context,
    private val supabase: SupabaseClient,
) {
    private val sessionManager by lazy { SharedPreferencesSessionManager(context) }

    /**
     * Gets the current user ID.
     * First tries Supabase auth, then falls back to stored session for offline support.
     */
    fun getCurrentUserId(): String? {
        // Try Supabase first (online mode)
        val supabaseUserId = supabase.auth.currentUserOrNull()?.id
        if (supabaseUserId != null) {
            return supabaseUserId
        }

        // Fallback to stored session (offline mode)
        return sessionManager.getStoredUserId()
    }

    /**
     * Returns true if there's a valid user ID available (either online or offline).
     */
    fun hasValidUser(): Boolean = getCurrentUserId() != null
}
