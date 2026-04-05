package com.bossxomlut.dragspend.domain.repository

interface SessionRepository {
    /** Returns the current authenticated user's ID, or null if not logged in. */
    fun getCurrentUserId(): String?

    /**
     * Returns a stable local user ID for data operations.
     * - Authenticated: returns the Supabase user ID.
     * - Guest: returns a locally generated UUID (persistent across app launches).
     */
    fun getLocalUserId(): String

    /** Returns the current authenticated user's email, or null if not logged in. */
    fun getCurrentUserEmail(): String?

    /** Returns true if there is an active authenticated session. */
    fun isAuthenticated(): Boolean

    /** Signs out the current user. */
    suspend fun signOut(): Result<Unit>
}
