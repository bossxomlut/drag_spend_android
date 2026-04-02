package com.bossxomlut.dragspend.domain.model

data class CardVariant(
    val id: String,
    val cardId: String,
    val label: String? = null,
    val amount: Long,
    val isDefault: Boolean,
    val position: Int = 0,
    val createdAt: String? = null,
)

data class SpendingCard(
    val id: String,
    val userId: String,
    val title: String,
    val categoryId: String? = null,
    val type: TransactionType,
    val note: String? = null,
    val position: Int = 0,
    val useCount: Int = 0,
    val language: String = "",
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val category: Category? = null,
    val variants: List<CardVariant>,
) {
    val defaultVariant: CardVariant?
        get() = variants.firstOrNull { it.isDefault } ?: variants.firstOrNull()

    val defaultAmount: Long
        get() = defaultVariant?.amount ?: 0L
}
