package com.bossxomlut.dragspend.domain.repository

import com.bossxomlut.dragspend.domain.model.SpendingCard
import com.bossxomlut.dragspend.domain.model.TransactionType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class CreateCardRequest(
    val userId: String,
    val title: String,
    val categoryId: String?,
    val type: TransactionType,
    val note: String?,
    val language: String,
    val variants: List<CreateVariantRequest>,
) {
    fun toJsonObject(): JsonObject = buildJsonObject {
        put("user_id", userId)
        put("title", title)
        categoryId?.let { put("category_id", it) }
        put("type", type.name.lowercase())
        note?.let { put("note", it) }
        put("language", language)
    }
}

data class CreateVariantRequest(
    val label: String?,
    val amount: Long,
    val isDefault: Boolean,
    val position: Int,
) {
    fun toJsonObject(cardId: String, position: Int): JsonObject = buildJsonObject {
        put("card_id", cardId)
        label?.let { put("label", it) }
        put("amount", amount)
        put("is_default", isDefault)
        put("position", position)
    }
}

interface CardRepository {
    suspend fun getCards(userId: String): Result<List<SpendingCard>>
    suspend fun createCard(request: CreateCardRequest): Result<SpendingCard>
    suspend fun updateCard(cardId: String, request: CreateCardRequest): Result<SpendingCard>
    suspend fun deleteCard(cardId: String): Result<Unit>
    suspend fun incrementUseCount(cardId: String): Result<Unit>
}
