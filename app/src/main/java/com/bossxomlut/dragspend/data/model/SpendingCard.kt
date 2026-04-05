package com.bossxomlut.dragspend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CardVariantDto(
    val id: String,
    @SerialName("card_id") val cardId: String,
    val label: String? = null,
    val amount: Long,
    @SerialName("is_default") val isDefault: Boolean? = null,
    val position: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class SpendingCardDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    @SerialName("category_id") val categoryId: String? = null,
    val type: TransactionTypeDto,
    val note: String? = null,
    val position: Int? = null,
    @SerialName("use_count") val useCount: Int = 0,
    val language: String = "vi",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
