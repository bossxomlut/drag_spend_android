package com.bossxomlut.dragspend.domain.usecase.profile

import com.bossxomlut.dragspend.domain.error.AppError
import com.bossxomlut.dragspend.domain.model.Profile
import com.bossxomlut.dragspend.domain.repository.ProfileRepository
import com.bossxomlut.dragspend.domain.repository.SessionRepository
import com.bossxomlut.dragspend.util.GuestSession

class GetProfileUseCase(
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository,
    private val guestSession: GuestSession,
) {
    suspend operator fun invoke(): Result<Profile> {
        val userId = sessionRepository.getCurrentUserId()
        if (userId == null) {
            // Guest mode: trả về profile synthetic với ngôn ngữ đã chọn
            return Result.success(
                Profile(
                    id = sessionRepository.getLocalUserId(),
                    name = null,
                    currency = "VND",
                    language = guestSession.getLanguage() ?: "vi",
                    isSeeded = true,
                    deletedAt = null,
                    createdAt = null,
                ),
            )
        }
        return profileRepository.getProfile(userId)
    }
}
