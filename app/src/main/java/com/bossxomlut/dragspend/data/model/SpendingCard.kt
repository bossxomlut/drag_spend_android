package com.bossxomlut.dragspend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CardVariant(
    val id: String,
    @SerialName("card_id") val cardId: String,
    val label: String? = null,
    val amount: Long,
    @SerialName("is_default") val isDefault: Boolean = false,
    val position: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class SpendingCard(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    @SerialName("category_id") val categoryId: String? = null,
    val type: TransactionType,
    val note: String? = null,
    val position: Int = 0,
    @SerialName("use_count") val useCount: Int = 0,
    val language: String = "vi",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val category: Category? = null,
    val variants: List<CardVariant> = emptyList(),
) {
    val defaultVariant: CardVariant?
        get() = variants.firstOrNull { it.isDefault } ?: variants.firstOrNull()

    val defaultAmount: Long
        get() = defaultVariant?.amount ?: 0L
}
