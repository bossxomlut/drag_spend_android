package com.bossxomlut.dragspend.util

import android.content.Context
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SharedPreferencesSessionManager(context: Context) : SessionManager {

    private val prefs = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveSession(session: UserSession) {
        prefs.edit().putString(KEY_SESSION, json.encodeToString(session)).apply()
    }

    override suspend fun loadSession(): UserSession? {
        val stored = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching { json.decodeFromString<UserSession>(stored) }.getOrNull()
    }

    override suspend fun deleteSession() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    /**
     * Loads the stored session directly without going through Supabase Auth.
     * Useful for offline mode when Supabase reports null session because token refresh failed.
     */
    fun loadSessionSync(): UserSession? {
        val stored = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching { json.decodeFromString<UserSession>(stored) }.getOrNull()
    }

    /**
     * Gets the stored user ID directly, even if the session is expired.
     * Returns null if no session was ever saved.
     */
    fun getStoredUserId(): String? {
        return loadSessionSync()?.user?.id
    }

    companion object {
        private const val KEY_SESSION = "session"
    }
}
