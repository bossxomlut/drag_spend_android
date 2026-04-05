package com.bossxomlut.dragspend.data.repository

import com.bossxomlut.dragspend.domain.repository.SessionRepository
import com.bossxomlut.dragspend.util.GuestSession
import com.bossxomlut.dragspend.util.ProfileCache
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

class SessionRepositoryImpl(
    private val supabase: SupabaseClient,
    private val profileCache: ProfileCache,
    private val guestSession: GuestSession,
) : SessionRepository {

    override fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    override fun getLocalUserId(): String =
        supabase.auth.currentUserOrNull()?.id ?: guestSession.getOrCreateGuestId()

    override fun getCurrentUserEmail(): String? = supabase.auth.currentUserOrNull()?.email

    override fun isAuthenticated(): Boolean = supabase.auth.currentUserOrNull() != null

    override suspend fun signOut(): Result<Unit> = runCatching {
        supabase.auth.signOut()
        profileCache.clear()
    }
}
