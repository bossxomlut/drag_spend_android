package com.bossxomlut.dragspend.data.repository

import com.bossxomlut.dragspend.data.model.MonthlyReportRowDto
import com.bossxomlut.dragspend.data.model.TransactionDto
import com.bossxomlut.dragspend.data.model.toDomain
import com.bossxomlut.dragspend.domain.error.mapToAppError
import com.bossxomlut.dragspend.domain.model.ReportEntry
import com.bossxomlut.dragspend.domain.model.Transaction
import com.bossxomlut.dragspend.domain.repository.CategoryRepository
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
            .decodeList<TransactionDto>()
        attachCategories(txns)
    }.logResult(AppLog.Feature.TRANSACTION, "getTransactions") { "${it.size} items" }
        .mapToAppError()

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
            .decodeList<TransactionDto>()
            .map { it.toDomain() }
    }.logResult(AppLog.Feature.TRANSACTION, "getMonthlyTransactions") { "${it.size} items" }
        .mapToAppError()

    override suspend fun getMonthlyReport(userId: String, yearMonth: String): Result<List<ReportEntry>> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "getMonthlyReport", "userId=${userId.take(8)}, yearMonth=$yearMonth")
        val params = buildJsonObject { put("p_year_month", yearMonth) }
        supabase.postgrest.rpc("get_monthly_report", params).decodeList<MonthlyReportRowDto>()
            .map { it.toDomain() }
    }.logResult(AppLog.Feature.TRANSACTION, "getMonthlyReport") { "${it.size} rows" }
        .mapToAppError()

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
            .decodeList<TransactionDto>()
            .firstOrNull()?.position ?: -1

        val row = request.toJsonObject(maxPosition + 1)
        supabase.from("transactions")
            .insert(row) { select() }
            .decodeSingle<TransactionDto>()
            .toDomain()
    }.logResult(AppLog.Feature.TRANSACTION, "createTransaction") { "id=${it.id}" }
        .mapToAppError()

    override suspend fun updateTransaction(
        transactionId: String,
        request: UpdateTransactionRequest,
    ): Result<Transaction> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "updateTransaction", "id=$transactionId, amount=${request.amount}, type=${request.type}")
        val row = request.toJsonObject()
        val updatedDto = supabase.from("transactions")
            .update(row) {
                filter { eq("id", transactionId) }
                select()
            }
            .decodeSingle<TransactionDto>()
        attachCategories(listOf(updatedDto)).first()
    }.logResult(AppLog.Feature.TRANSACTION, "updateTransaction") { "id=${it.id}" }
        .mapToAppError()

    override suspend fun deleteTransaction(transactionId: String): Result<Unit> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "deleteTransaction", "id=$transactionId")
        supabase.from("transactions")
            .delete { filter { eq("id", transactionId) } }
        Unit
    }.logResult(AppLog.Feature.TRANSACTION, "deleteTransaction") { "deleted" }
        .mapToAppError()

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
            .decodeList<TransactionDto>()

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
            .decodeList<TransactionDto>()
            .firstOrNull()?.position ?: -1

        val newRows = yesterday.mapIndexed { index, t ->
            CreateTransactionRequest(
                userId = userId,
                sourceCardId = t.sourceCardId,
                date = toDate,
                title = t.title,
                amount = t.amount,
                categoryId = t.categoryId,
                type = t.type.toDomain(),
                note = t.note,
            ).toJsonObject(maxPosition + 1 + index)
        }
        supabase.from("transactions")
            .insert(newRows) { select() }
            .decodeList<TransactionDto>()
            .let { attachCategories(it) }
    }.logResult(AppLog.Feature.TRANSACTION, "copyFromYesterday") { "${it.size} copied" }
        .mapToAppError()

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

        // When exactly one category is selected we push it down to the RPC so
        // the DB can filter early. For multiple categories we skip p_category_id
        // and apply client-side filtering on the (already date+text-filtered) result.
        val singleCategoryId = if (categoryIds.size == 1) categoryIds.first() else null

        val params = buildJsonObject {
            put("p_user_id", userId)
            put("p_query", query)
            if (startDate != null) put("p_date_from", startDate)
            if (endDate != null) put("p_date_to", endDate)
            if (singleCategoryId != null) put("p_category_id", singleCategoryId)
            put("p_limit", 500)
            put("p_offset", 0)
        }

        val txns = supabase.postgrest
            .rpc("search_transactions_unaccent", params)
            .decodeList<TransactionDto>()

        AppLog.d(
            AppLog.Feature.TRANSACTION,
            "searchTransactions",
            "RPC returned ${txns.size} rows",
        )

        val results = attachCategories(txns)

        // Apply multi-category filter client-side (single-category already handled by RPC)
        if (categoryIds.size > 1) results.filter { it.categoryId in categoryIds } else results
    }.logResult(AppLog.Feature.TRANSACTION, "searchTransactions") { "${it.size} items" }
        .mapToAppError()

    private suspend fun attachCategories(transactionDtos: List<TransactionDto>): List<Transaction> {
        if (transactionDtos.isEmpty()) return emptyList()
        val catIds = transactionDtos.mapNotNull { it.categoryId }.distinct()
        if (catIds.isEmpty()) return transactionDtos.map { it.toDomain() }
        val userId = transactionDtos.first().userId
        val allCategories = categoryRepository.getCategories(userId).getOrElse { emptyList() }
        val categoryById = allCategories.associateBy { it.id }
        return transactionDtos.map { dto ->
            dto.toDomain(category = dto.categoryId?.let { categoryById[it] })
        }
    }
}
