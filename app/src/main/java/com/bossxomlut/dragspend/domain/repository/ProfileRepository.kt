package com.bossxomlut.dragspend.domain.repository

import com.bossxomlut.dragspend.data.model.Profile

interface ProfileRepository {
    suspend fun getProfile(userId: String): Result<Profile>
    suspend fun ensureUserSeeded(userId: String, name: String?, language: String): Result<Boolean>
    suspend fun updateLanguage(userId: String, language: String): Result<Unit>
    suspend fun updateName(userId: String, name: String): Result<Unit>
    suspend fun softDeleteAccount(userId: String): Result<Unit>
}
