package com.bossxomlut.dragspend.domain.model

data class Transaction(
    val id: String,
    val userId: String,
    val sourceCardId: String? = null,
    val date: String,
    val title: String,
    val amount: Long,
    val categoryId: String? = null,
    val type: TransactionType,
    val note: String? = null,
    val position: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val category: Category? = null,
)

data class DayTotal(
    val date: String,
    val income: Long,
    val expense: Long,
) {
    val net: Long get() = income - expense
}

/** Aggregated report row — one entry per (date, category, type) returned by the monthly report RPC. */
data class ReportEntry(
    val date: String,
    val categoryId: String?,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?,
    val type: TransactionType,
    val total: Long,
)
