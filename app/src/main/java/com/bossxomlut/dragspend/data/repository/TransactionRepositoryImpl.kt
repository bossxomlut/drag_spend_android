package com.bossxomlut.dragspend.data.repository

import com.bossxomlut.dragspend.data.model.MonthlyReportRow
import com.bossxomlut.dragspend.data.model.Transaction
import com.bossxomlut.dragspend.domain.repository.CreateTransactionRequest
import com.bossxomlut.dragspend.domain.repository.TransactionRepository
import com.bossxomlut.dragspend.domain.repository.UpdateTransactionRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TransactionRepositoryImpl(
    private val supabase: SupabaseClient,
) : TransactionRepository {

    override suspend fun getTransactions(userId: String, date: String): Result<List<Transaction>> = runCatching {
        supabase.from("transactions")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("date", date)
                }
                order("position", order = Order.ASCENDING)
            }
            .decodeList<Transaction>()
    }

    override suspend fun getMonthlyTransactions(userId: String, yearMonth: String): Result<List<Transaction>> = runCatching {
        val startDate = "$yearMonth-01"
        val nextMonth = if (yearMonth.endsWith("12")) {
            "${yearMonth.take(4).toInt() + 1}-01"
        } else {
            val parts = yearMonth.split("-")
            "${parts[0]}-${(parts[1].toInt() + 1).toString().padStart(2, '0')}"
        }
        val endDate = "$nextMonth-01"

        supabase.from("transactions")
            .select {
                filter {
                    eq("user_id", userId)
                    gte("date", startDate)
                    lt("date", endDate)
                }
                order("date", order = Order.ASCENDING)
                order("position", order = Order.ASCENDING)
            }
            .decodeList<Transaction>()
    }

    override suspend fun getMonthlyReport(userId: String, yearMonth: String): Result<List<MonthlyReportRow>> = runCatching {
        val params = buildJsonObject { put("p_year_month", yearMonth) }
        supabase.postgrest.rpc("get_monthly_report", params).decodeList<MonthlyReportRow>()
    }

    override suspend fun createTransaction(request: CreateTransactionRequest): Result<Transaction> = runCatching {
        val maxPosition = supabase.from("transactions")
            .select {
                filter {
                    eq("user_id", request.userId)
                    eq("date", request.date)
                }
                order("position", order = Order.DESCENDING)
                limit(count = 1)
            }
            .decodeList<Transaction>()
            .firstOrNull()?.position ?: -1

        val row = mapOf(
            "user_id" to request.userId,
            "source_card_id" to request.sourceCardId,
            "date" to request.date,
            "title" to request.title,
            "amount" to request.amount,
            "category_id" to request.categoryId,
            "type" to request.type.name.lowercase(),
            "note" to request.note,
            "position" to (maxPosition + 1),
        )
        supabase.from("transactions")
            .insert(row) { select() }
            .decodeSingle<Transaction>()
    }

    override suspend fun updateTransaction(
        transactionId: String,
        request: UpdateTransactionRequest,
    ): Result<Transaction> = runCatching {
        val row = mapOf(
            "title" to request.title,
            "amount" to request.amount,
            "category_id" to request.categoryId,
            "type" to request.type.name.lowercase(),
            "note" to request.note,
            "date" to request.date,
        )
        supabase.from("transactions")
            .update(row) {
                filter { eq("id", transactionId) }
                select()
            }
            .decodeSingle<Transaction>()
    }

    override suspend fun deleteTransaction(transactionId: String): Result<Unit> = runCatching {
        supabase.from("transactions")
            .delete { filter { eq("id", transactionId) } }
        Unit
    }

    override suspend fun copyFromYesterday(
        userId: String,
        fromDate: String,
        toDate: String,
    ): Result<List<Transaction>> = runCatching {
        val yesterday = supabase.from("transactions")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("date", fromDate)
                }
                order("position", order = Order.ASCENDING)
            }
            .decodeList<Transaction>()

        if (yesterday.isEmpty()) return@runCatching emptyList()

        val maxPosition = supabase.from("transactions")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("date", toDate)
                }
                order("position", order = Order.DESCENDING)
                limit(count = 1)
            }
            .decodeList<Transaction>()
            .firstOrNull()?.position ?: -1

        val newRows = yesterday.mapIndexed { index, t ->
            mapOf(
                "user_id" to userId,
                "source_card_id" to t.sourceCardId,
                "date" to toDate,
                "title" to t.title,
                "amount" to t.amount,
                "category_id" to t.categoryId,
                "type" to t.type.name.lowercase(),
                "note" to t.note,
                "position" to (maxPosition + 1 + index),
            )
        }
        supabase.from("transactions")
            .insert(newRows) { select() }
            .decodeList<Transaction>()
    }
}
