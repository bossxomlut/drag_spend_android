package com.bossxomlut.dragspend.domain.usecase.card

import com.bossxomlut.dragspend.domain.repository.CardRepository

class IncrementCardUseCountUseCase(
    private val cardRepository: CardRepository,
) {
    suspend operator fun invoke(cardId: String): Result<Unit> =
        cardRepository.incrementUseCount(cardId)
}
