package com.bossxomlut.dragspend.data.repository

import com.bossxomlut.dragspend.data.model.MonthlyReportRow
import com.bossxomlut.dragspend.data.model.Transaction
import com.bossxomlut.dragspend.domain.repository.CreateTransactionRequest
import com.bossxomlut.dragspend.domain.repository.TransactionRepository
import com.bossxomlut.dragspend.domain.repository.UpdateTransactionRequest
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.logResult
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
        AppLog.d(AppLog.Feature.TRANSACTION, "getTransactions", "userId=${userId.take(8)}, date=$date")
        supabase.from("transactions")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("date", date)
                }
                order("position", order = Order.ASCENDING)
            }
            .decodeList<Transaction>()
    }.logResult(AppLog.Feature.TRANSACTION, "getTransactions") { "${it.size} items" }

    override suspend fun getMonthlyTransactions(userId: String, yearMonth: String): Result<List<Transaction>> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "getMonthlyTransactions", "userId=${userId.take(8)}, yearMonth=$yearMonth")
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
    }.logResult(AppLog.Feature.TRANSACTION, "getMonthlyTransactions") { "${it.size} items" }

    override suspend fun getMonthlyReport(userId: String, yearMonth: String): Result<List<MonthlyReportRow>> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "getMonthlyReport", "userId=${userId.take(8)}, yearMonth=$yearMonth")
        val params = buildJsonObject { put("p_year_month", yearMonth) }
        supabase.postgrest.rpc("get_monthly_report", params).decodeList<MonthlyReportRow>()
    }.logResult(AppLog.Feature.TRANSACTION, "getMonthlyReport") { "${it.size} rows" }

    override suspend fun createTransaction(request: CreateTransactionRequest): Result<Transaction> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "createTransaction", "date=${request.date}, title=${request.title}, amount=${request.amount}, type=${request.type}")
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

        val row = buildJsonObject {
            put("user_id", request.userId)
            request.sourceCardId?.let { put("source_card_id", it) }
            put("date", request.date)
            put("title", request.title)
            put("amount", request.amount)
            request.categoryId?.let { put("category_id", it) }
            put("type", request.type.name.lowercase())
            request.note?.let { put("note", it) }
            put("position", maxPosition + 1)
        }
        supabase.from("transactions")
            .insert(row) { select() }
            .decodeSingle<Transaction>()
    }.logResult(AppLog.Feature.TRANSACTION, "createTransaction") { "id=${it.id}" }

    override suspend fun updateTransaction(
        transactionId: String,
        request: UpdateTransactionRequest,
    ): Result<Transaction> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "updateTransaction", "id=$transactionId, amount=${request.amount}, type=${request.type}")
        val row = buildJsonObject {
            put("title", request.title)
            put("amount", request.amount)
            request.categoryId?.let { put("category_id", it) }
            put("type", request.type.name.lowercase())
            request.note?.let { put("note", it) }
            put("date", request.date)
            request.sourceCardId?.let { put("source_card_id", it) }
        }
        supabase.from("transactions")
            .update(row) {
                filter { eq("id", transactionId) }
                select()
            }
            .decodeSingle<Transaction>()
    }.logResult(AppLog.Feature.TRANSACTION, "updateTransaction") { "id=${it.id}" }

    override suspend fun deleteTransaction(transactionId: String): Result<Unit> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "deleteTransaction", "id=$transactionId")
        supabase.from("transactions")
            .delete { filter { eq("id", transactionId) } }
        Unit
    }.logResult(AppLog.Feature.TRANSACTION, "deleteTransaction") { "deleted" }

    override suspend fun copyFromYesterday(
        userId: String,
        fromDate: String,
        toDate: String,
    ): Result<List<Transaction>> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "copyFromYesterday", "from=$fromDate, to=$toDate")
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
            buildJsonObject {
                put("user_id", userId)
                t.sourceCardId?.let { put("source_card_id", it) }
                put("date", toDate)
                put("title", t.title)
                put("amount", t.amount)
                t.categoryId?.let { put("category_id", it) }
                put("type", t.type.name.lowercase())
                t.note?.let { put("note", it) }
                put("position", maxPosition + 1 + index)
            }
        }
        supabase.from("transactions")
            .insert(newRows) { select() }
            .decodeList<Transaction>()
    }.logResult(AppLog.Feature.TRANSACTION, "copyFromYesterday") { "${it.size} copied" }
}
