package com.bossxomlut.dragspend.domain.repository

interface SessionRepository {
    /** Returns the current authenticated user's ID, or null if not logged in. */
    fun getCurrentUserId(): String?

    /** Returns the current authenticated user's email, or null if not logged in. */
    fun getCurrentUserEmail(): String?

    /** Returns true if there is an active authenticated session. */
    fun isAuthenticated(): Boolean

    /** Signs out the current user. */
    suspend fun signOut(): Result<Unit>
}
