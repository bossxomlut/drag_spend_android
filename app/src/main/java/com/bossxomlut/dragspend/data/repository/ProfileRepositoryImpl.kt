package com.bossxomlut.dragspend.data.repository

import com.bossxomlut.dragspend.data.model.ProfileDto
import com.bossxomlut.dragspend.data.model.toDomain
import com.bossxomlut.dragspend.domain.error.mapToAppError
import com.bossxomlut.dragspend.domain.model.Profile
import com.bossxomlut.dragspend.domain.repository.ProfileRepository
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.logResult
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
        AppLog.d(AppLog.Feature.PROFILE, "getProfile", "userId=${userId.take(8)}")
        supabase.from("profiles")
            .select {
                filter { eq("id", userId) }
            }
            .decodeSingle<ProfileDto>()
            .toDomain()
    }.logResult(AppLog.Feature.PROFILE, "getProfile") { "name=${it.name}" }
        .mapToAppError()

    override suspend fun ensureUserSeeded(
        userId: String,
        name: String?,
        language: String,
    ): Result<Boolean> = runCatching {
        AppLog.d(AppLog.Feature.PROFILE, "ensureUserSeeded", "userId=${userId.take(8)}, language=$language")
        val params = buildJsonObject {
            put("p_user_id", userId)
            put("p_name", name ?: "")
            put("p_language", language)
        }
        supabase.postgrest.rpc("ensure_user_seeded", params).decodeSingle<Boolean>()
    }.logResult(AppLog.Feature.PROFILE, "ensureUserSeeded") { "seeded=$it" }
        .mapToAppError()

    override suspend fun updateLanguage(userId: String, language: String): Result<Unit> = runCatching {
        AppLog.d(AppLog.Feature.PROFILE, "updateLanguage", "language=$language")
        supabase.from("profiles")
            .update(mapOf("language" to language)) {
                filter { eq("id", userId) }
            }
        Unit
    }.logResult(AppLog.Feature.PROFILE, "updateLanguage") { "ok" }
        .mapToAppError()

    override suspend fun updateName(userId: String, name: String): Result<Unit> = runCatching {
        AppLog.d(AppLog.Feature.PROFILE, "updateName", "name=$name")
        supabase.from("profiles")
            .update(mapOf("name" to name)) {
                filter { eq("id", userId) }
            }
        Unit
    }.logResult(AppLog.Feature.PROFILE, "updateName") { "ok" }
        .mapToAppError()

    override suspend fun softDeleteAccount(userId: String): Result<Unit> = runCatching {
        AppLog.d(AppLog.Feature.PROFILE, "softDeleteAccount", "userId=${userId.take(8)}")
        supabase.from("profiles")
            .update(mapOf("deleted_at" to "now()")) {
                filter { eq("id", userId) }
            }
        Unit
    }.logResult(AppLog.Feature.PROFILE, "softDeleteAccount") { "ok" }
        .mapToAppError()
}
