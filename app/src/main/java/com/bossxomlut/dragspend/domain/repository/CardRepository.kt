package com.bossxomlut.dragspend.domain.repository

import com.bossxomlut.dragspend.data.model.CardVariant
import com.bossxomlut.dragspend.data.model.SpendingCard
import com.bossxomlut.dragspend.data.model.TransactionType

data class CreateCardRequest(
    val userId: String,
    val title: String,
    val categoryId: String?,
    val type: TransactionType,
    val note: String?,
    val language: String,
    val variants: List<CreateVariantRequest>,
)

data class CreateVariantRequest(
    val label: String?,
    val amount: Long,
    val isDefault: Boolean,
    val position: Int,
)

interface CardRepository {
    suspend fun getCards(userId: String): Result<List<SpendingCard>>
    suspend fun createCard(request: CreateCardRequest): Result<SpendingCard>
    suspend fun updateCard(cardId: String, request: CreateCardRequest): Result<SpendingCard>
    suspend fun deleteCard(cardId: String): Result<Unit>
    suspend fun incrementUseCount(cardId: String): Result<Unit>
}
