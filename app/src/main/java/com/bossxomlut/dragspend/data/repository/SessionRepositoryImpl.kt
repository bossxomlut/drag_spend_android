package com.bossxomlut.dragspend.data.repository

import com.bossxomlut.dragspend.domain.repository.SessionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

class SessionRepositoryImpl(
    private val supabase: SupabaseClient,
) : SessionRepository {

    override fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    override fun getCurrentUserEmail(): String? = supabase.auth.currentUserOrNull()?.email

    override fun isAuthenticated(): Boolean = supabase.auth.currentUserOrNull() != null

    override suspend fun signOut(): Result<Unit> = runCatching { supabase.auth.signOut() }
}
