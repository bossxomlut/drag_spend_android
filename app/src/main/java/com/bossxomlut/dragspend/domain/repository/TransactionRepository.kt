package com.bossxomlut.dragspend.domain.repository

import com.bossxomlut.dragspend.data.model.MonthlyReportRow
import com.bossxomlut.dragspend.data.model.Transaction
import com.bossxomlut.dragspend.data.model.TransactionType

data class CreateTransactionRequest(
    val userId: String,
    val sourceCardId: String?,
    val date: String,
    val title: String,
    val amount: Long,
    val categoryId: String?,
    val type: TransactionType,
    val note: String?,
)

data class UpdateTransactionRequest(
    val title: String,
    val amount: Long,
    val categoryId: String?,
    val type: TransactionType,
    val note: String?,
    val date: String,
)

interface TransactionRepository {
    suspend fun getTransactions(userId: String, date: String): Result<List<Transaction>>
    suspend fun getMonthlyTransactions(userId: String, yearMonth: String): Result<List<Transaction>>
    suspend fun getMonthlyReport(userId: String, yearMonth: String): Result<List<MonthlyReportRow>>
    suspend fun createTransaction(request: CreateTransactionRequest): Result<Transaction>
    suspend fun updateTransaction(transactionId: String, request: UpdateTransactionRequest): Result<Transaction>
    suspend fun deleteTransaction(transactionId: String): Result<Unit>
    suspend fun copyFromYesterday(userId: String, fromDate: String, toDate: String): Result<List<Transaction>>
}
