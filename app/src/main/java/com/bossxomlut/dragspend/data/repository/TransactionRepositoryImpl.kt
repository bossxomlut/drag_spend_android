package com.bossxomlut.dragspend.data.repository

import com.bossxomlut.dragspend.data.model.MonthlyReportRow
import com.bossxomlut.dragspend.data.model.Transaction
import com.bossxomlut.dragspend.domain.repository.CategoryRepository
import com.bossxomlut.dragspend.domain.repository.CreateTransactionRequest
import com.bossxomlut.dragspend.domain.repository.TransactionRepository
import com.bossxomlut.dragspend.domain.repository.UpdateTransactionRequest
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.logResult
import io.github.jan.supabase.SupabaseClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TransactionRepositoryImpl(
    private val supabase: SupabaseClient,
    private val categoryRepository: CategoryRepository,
) : TransactionRepository {

    override suspend fun getTransactions(userId: String, date: String): Result<List<Transaction>> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "getTransactions", "userId=${userId.take(8)}, date=$date")
        val txns = supabase.from("transactions")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("date", date)
                }
                order("position", order = Order.ASCENDING)
            }
            .decodeList<Transaction>()
        attachCategories(txns)
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

        val row = request.toJsonObject(maxPosition + 1)
        supabase.from("transactions")
            .insert(row) { select() }
            .decodeSingle<Transaction>()
    }.logResult(AppLog.Feature.TRANSACTION, "createTransaction") { "id=${it.id}" }

    override suspend fun updateTransaction(
        transactionId: String,
        request: UpdateTransactionRequest,
    ): Result<Transaction> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "updateTransaction", "id=$transactionId, amount=${request.amount}, type=${request.type}")
        val row = request.toJsonObject()
        val updated = supabase.from("transactions")
            .update(row) {
                filter { eq("id", transactionId) }
                select()
            }
            .decodeSingle<Transaction>()
        attachCategories(listOf(updated)).first()
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
            CreateTransactionRequest(
                userId = userId,
                sourceCardId = t.sourceCardId,
                date = toDate,
                title = t.title,
                amount = t.amount,
                categoryId = t.categoryId,
                type = t.type,
                note = t.note,
            ).toJsonObject(maxPosition + 1 + index)
        }
        supabase.from("transactions")
            .insert(newRows) { select() }
            .decodeList<Transaction>()
            .let { attachCategories(it) }
    }.logResult(AppLog.Feature.TRANSACTION, "copyFromYesterday") { "${it.size} copied" }

    override suspend fun searchTransactions(
        userId: String,
        query: String,
        categoryIds: Set<String>,
        startDate: String?,
        endDate: String?,
    ): Result<List<Transaction>> = runCatching {
        AppLog.d(
            AppLog.Feature.TRANSACTION,
            "searchTransactions",
            "userId=${userId.take(8)}, query=$query, categories=$categoryIds, start=$startDate, end=$endDate",
        )

        // Both dates should always be provided by SearchViewModel (with 3-month default fallback).
        // Convert endDate to exclusive (endDate + 1 day) for lt() operator.
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val start = startDate ?: LocalDate.now().minusMonths(3).format(dateFormatter)
        val exclusiveEnd = endDate?.let {
            LocalDate.parse(it, dateFormatter).plusDays(1).format(dateFormatter)
        } ?: LocalDate.now().plusDays(1).format(dateFormatter)

        AppLog.d(
            AppLog.Feature.TRANSACTION,
            "searchTransactions",
            "Querying with start=$start, exclusiveEnd=$exclusiveEnd",
        )

        val txns = supabase.from("transactions")
            .select {
                filter {
                    eq("user_id", userId)
                    gte("date", start)
                    lt("date", exclusiveEnd)
                }
                order("date", order = Order.DESCENDING)
                order("position", order = Order.ASCENDING)
            }
            .decodeList<Transaction>()

        AppLog.d(
            AppLog.Feature.TRANSACTION,
            "searchTransactions",
            "Server returned ${txns.size} transactions",
        )

        val withCategories = attachCategories(txns)

        // Client-side filtering (text, category, AND dates as fallback safety)
        withCategories.filter { tx ->
            val matchesQuery = query.isBlank() || tx.title.contains(query, ignoreCase = true)
            val matchesCategory = categoryIds.isEmpty() || tx.categoryId in categoryIds
            val matchesDateRange = tx.date >= start && tx.date < exclusiveEnd
            matchesQuery && matchesCategory && matchesDateRange
        }
    }.logResult(AppLog.Feature.TRANSACTION, "searchTransactions") { "${it.size} items" }

    /**
     * Attaches the cached Category object to each transaction that has a category_id.
     * Uses CategoryRepository so the look-up is served from the in-memory cache on subsequent calls.
     */
    private suspend fun attachCategories(transactions: List<Transaction>): List<Transaction> {
        if (transactions.isEmpty()) return transactions
        val categoryIds = transactions.mapNotNull { it.categoryId }.distinct()
        if (categoryIds.isEmpty()) return transactions
        // All transactions belong to the same user — use the first one to get the userId.
        val userId = transactions.first().userId
        val allCategories = categoryRepository.getCategories(userId).getOrElse { emptyList() }
        val categoryById = allCategories.associateBy { it.id }
        return transactions.map { tx ->
            tx.copy(category = tx.categoryId?.let { categoryById[it] })
        }
    }
}
