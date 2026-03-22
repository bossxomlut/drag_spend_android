package com.bossxomlut.dragspend.domain.repository

import com.bossxomlut.dragspend.data.model.MonthlyReportRow
import com.bossxomlut.dragspend.data.model.Transaction
import com.bossxomlut.dragspend.data.model.TransactionType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class CreateTransactionRequest(
    val userId: String,
    val sourceCardId: String?,
    val date: String,
    val title: String,
    val amount: Long,
    val categoryId: String?,
    val type: TransactionType,
    val note: String?,
) {
    fun toJsonObject(position: Int): JsonObject = buildJsonObject {
        put("user_id", userId)
        sourceCardId?.let { put("source_card_id", it) }
        put("date", date)
        put("title", title)
        put("amount", amount)
        categoryId?.let { put("category_id", it) }
        put("type", type.name.lowercase())
        note?.let { put("note", it) }
        put("position", position)
    }
}

data class UpdateTransactionRequest(
    val title: String,
    val amount: Long,
    val categoryId: String?,
    val type: TransactionType,
    val note: String?,
    val date: String,
    val sourceCardId: String? = null,
) {
    fun toJsonObject(): JsonObject = buildJsonObject {
        put("title", title)
        put("amount", amount)
        categoryId?.let { put("category_id", it) }
        put("type", type.name.lowercase())
        note?.let { put("note", it) }
        put("date", date)
        sourceCardId?.let { put("source_card_id", it) }
    }
}

interface TransactionRepository {
    suspend fun getTransactions(userId: String, date: String): Result<List<Transaction>>
    suspend fun getMonthlyTransactions(userId: String, yearMonth: String): Result<List<Transaction>>
    suspend fun getMonthlyReport(userId: String, yearMonth: String): Result<List<MonthlyReportRow>>
    suspend fun createTransaction(request: CreateTransactionRequest): Result<Transaction>
    suspend fun updateTransaction(transactionId: String, request: UpdateTransactionRequest): Result<Transaction>
    suspend fun deleteTransaction(transactionId: String): Result<Unit>
    suspend fun copyFromYesterday(userId: String, fromDate: String, toDate: String): Result<List<Transaction>>
    suspend fun searchTransactions(
        userId: String,
        query: String,
        categoryIds: Set<String>,
        startDate: String?,
        endDate: String?,
    ): Result<List<Transaction>>
}
