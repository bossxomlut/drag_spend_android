package com.bossxomlut.dragspend.domain.usecase.profile

import com.bossxomlut.dragspend.domain.error.AppError
import com.bossxomlut.dragspend.domain.repository.ProfileRepository
import com.bossxomlut.dragspend.domain.repository.SessionRepository

class DeleteAccountUseCase(
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        val userId = sessionRepository.getCurrentUserId()
            ?: return Result.failure(AppError.Unauthorized)
        return profileRepository.softDeleteAccount(userId)
    }
}
