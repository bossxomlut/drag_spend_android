package com.bossxomlut.dragspend.data.repository

import com.bossxomlut.dragspend.data.model.Profile
import com.bossxomlut.dragspend.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ProfileRepositoryImpl(
    private val supabase: SupabaseClient,
) : ProfileRepository {

    override suspend fun getProfile(userId: String): Result<Profile> = runCatching {
        supabase.from("profiles")
            .select {
                filter { eq("id", userId) }
            }
            .decodeSingle<Profile>()
    }

    override suspend fun ensureUserSeeded(
        userId: String,
        name: String?,
        language: String,
    ): Result<Boolean> = runCatching {
        val params = buildJsonObject {
            put("p_user_id", userId)
            put("p_name", name ?: "")
            put("p_language", language)
        }
        supabase.postgrest.rpc("ensure_user_seeded", params).decodeSingle<Boolean>()
    }

    override suspend fun updateLanguage(userId: String, language: String): Result<Unit> = runCatching {
        supabase.from("profiles")
            .update(mapOf("language" to language)) {
                filter { eq("id", userId) }
            }
        Unit
    }

    override suspend fun softDeleteAccount(userId: String): Result<Unit> = runCatching {
        supabase.from("profiles")
            .update(mapOf("deleted_at" to "now()")) {
                filter { eq("id", userId) }
            }
        Unit
    }
}
