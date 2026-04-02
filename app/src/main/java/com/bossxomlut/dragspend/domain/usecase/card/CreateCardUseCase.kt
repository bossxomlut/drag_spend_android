package com.bossxomlut.dragspend.domain.usecase.card

import com.bossxomlut.dragspend.domain.model.SpendingCard
import com.bossxomlut.dragspend.domain.repository.CardRepository
import com.bossxomlut.dragspend.domain.repository.CreateCardRequest

class CreateCardUseCase(
    private val cardRepository: CardRepository,
) {
    suspend operator fun invoke(request: CreateCardRequest): Result<SpendingCard> =
        cardRepository.createCard(request)
}
