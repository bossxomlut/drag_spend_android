package com.bossxomlut.dragspend.data.model

import com.bossxomlut.dragspend.data.local.entity.SyncStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Transaction(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("source_card_id") val sourceCardId: String? = null,
    val date: String,
    val title: String,
    val amount: Long,
    @SerialName("category_id") val categoryId: String? = null,
    val type: TransactionType,
    val note: String? = null,
    val position: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val category: Category? = null,
    // Not serialized - only used for local UI state
    @Transient val syncStatus: SyncStatus = SyncStatus.SYNCED,
)

data class DayTotal(
    val date: String,
    val income: Long,
    val expense: Long,
) {
    val net: Long get() = income - expense
}

@Serializable
data class MonthlyReportRow(
    val date: String,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("category_icon") val categoryIcon: String? = null,
    @SerialName("category_color") val categoryColor: String? = null,
    val type: TransactionType,
    val total: Long,
)
