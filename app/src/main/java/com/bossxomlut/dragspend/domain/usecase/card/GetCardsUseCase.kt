package com.bossxomlut.dragspend.domain.usecase.card

import com.bossxomlut.dragspend.domain.error.AppError
import com.bossxomlut.dragspend.domain.model.SpendingCard
import com.bossxomlut.dragspend.domain.repository.CardRepository
import com.bossxomlut.dragspend.domain.repository.SessionRepository

class GetCardsUseCase(
    private val cardRepository: CardRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(): Result<List<SpendingCard>> {
        val userId = sessionRepository.getCurrentUserId()
            ?: return Result.failure(AppError.Unauthorized)
        return cardRepository.getCards(userId)
    }
}
