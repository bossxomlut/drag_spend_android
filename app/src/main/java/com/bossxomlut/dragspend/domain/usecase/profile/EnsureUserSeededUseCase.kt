package com.bossxomlut.dragspend.domain.usecase.profile

import com.bossxomlut.dragspend.data.local.LocalSeeder
import com.bossxomlut.dragspend.domain.repository.ProfileRepository
import com.bossxomlut.dragspend.domain.repository.SessionRepository

class EnsureUserSeededUseCase(
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository,
    private val localSeeder: LocalSeeder,
) {
    suspend operator fun invoke(name: String?, language: String): Result<Boolean> {
        val userId = sessionRepository.getCurrentUserId()
        if (userId == null) {
            // Guest mode: tạo dữ liệu mặc định trong Room
            val guestId = sessionRepository.getLocalUserId()
            localSeeder.seedFor(guestId, language)
            return Result.success(true)
        }
        return profileRepository.ensureUserSeeded(userId, name, language)
    }
}
