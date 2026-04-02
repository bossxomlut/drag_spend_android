package com.bossxomlut.dragspend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("source_card_id") val sourceCardId: String? = null,
    val date: String,
    val title: String,
    val amount: Long,
    @SerialName("category_id") val categoryId: String? = null,
    val type: TransactionTypeDto,
    val note: String? = null,
    val position: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class MonthlyReportRowDto(
    val date: String,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("category_icon") val categoryIcon: String? = null,
    @SerialName("category_color") val categoryColor: String? = null,
    val type: TransactionTypeDto,
    val total: Long,
)
