package com.bossxomlut.dragspend.data.repository

import com.bossxomlut.dragspend.data.local.dao.CategoryDao
import com.bossxomlut.dragspend.data.local.dao.TransactionDao
import com.bossxomlut.dragspend.data.local.entity.TransactionEntity
import com.bossxomlut.dragspend.data.local.toDomain
import com.bossxomlut.dragspend.data.local.toEntity
import com.bossxomlut.dragspend.data.model.TransactionDto
import com.bossxomlut.dragspend.data.model.toDomain
import com.bossxomlut.dragspend.domain.error.mapToAppError
import com.bossxomlut.dragspend.domain.model.Category
import com.bossxomlut.dragspend.domain.model.ReportEntry
import com.bossxomlut.dragspend.domain.model.Transaction
import com.bossxomlut.dragspend.domain.repository.CategoryRepository
import com.bossxomlut.dragspend.domain.repository.CreateTransactionRequest
import com.bossxomlut.dragspend.domain.repository.SessionRepository
import com.bossxomlut.dragspend.domain.repository.TransactionRepository
import com.bossxomlut.dragspend.domain.repository.UpdateTransactionRequest
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.logResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TransactionRepositoryImpl(
    private val supabase: SupabaseClient,
    private val categoryRepository: CategoryRepository,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val sessionRepository: SessionRepository,
) : TransactionRepository {

    override suspend fun getTransactions(userId: String, date: String): Result<List<Transaction>> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "getTransactions", "userId=${userId.take(8)}, date=$date")
        attachCategoriesLocal(transactionDao.getByDate(userId, date), userId)
    }.logResult(AppLog.Feature.TRANSACTION, "getTransactions") { "${it.size} items" }
        .mapToAppError()

    override suspend fun getMonthlyTransactions(userId: String, yearMonth: String): Result<List<Transaction>> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "getMonthlyTransactions", "userId=${userId.take(8)}, yearMonth=$yearMonth")
        val (startDate, endDate) = monthRange(yearMonth)
        transactionDao.getByDateRange(userId, startDate, endDate).map { it.toDomain() }
    }.logResult(AppLog.Feature.TRANSACTION, "getMonthlyTransactions") { "${it.size} items" }
        .mapToAppError()

    override suspend fun getMonthlyReport(userId: String, yearMonth: String): Result<List<ReportEntry>> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "getMonthlyReport", "userId=${userId.take(8)}, yearMonth=$yearMonth")
        val (startDate, endDate) = monthRange(yearMonth)
        transactionDao.getMonthlyReport(userId, startDate, endDate).map { it.toDomain() }
    }.logResult(AppLog.Feature.TRANSACTION, "getMonthlyReport") { "${it.size} rows" }
        .mapToAppError()

    override suspend fun createTransaction(request: CreateTransactionRequest): Result<Transaction> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "createTransaction", "date=${request.date}, amount=${request.amount}")
        val localId = UUID.randomUUID().toString()

        if (!sessionRepository.isAuthenticated()) {
            val maxPos = transactionDao.getMaxPositionForDate(request.userId, request.date) ?: -1
            val entity = TransactionEntity(
                id = localId,
                userId = request.userId,
                sourceCardId = request.sourceCardId,
                date = request.date,
                title = request.title,
                amount = request.amount,
                categoryId = request.categoryId,
                type = request.type.name.lowercase(),
                note = request.note,
                position = maxPos + 1,
                createdAt = Instant.now().toString(),
                updatedAt = null,
                syncedAt = null,
                deletedAt = null,
            )
            transactionDao.upsert(entity)
            val category = request.categoryId?.let { categoryDao.getById(it)?.toDomain() }
            return@runCatching entity.toDomain(category = category)
        }

        val maxPosition = transactionDao.getMaxPositionForDate(request.userId, request.date) ?: -1

        val row = buildJsonObject {
            put("id", localId)
            request.toJsonObject(maxPosition + 1).entries.forEach { (k, v) -> put(k, v) }
        }
        val dto = supabase.from("transactions")
            .insert(row) { select() }
            .decodeSingle<TransactionDto>()
        transactionDao.upsert(dto.toEntity(syncedAt = Instant.now().toString()))
        dto.toDomain()
    }.logResult(AppLog.Feature.TRANSACTION, "createTransaction") { "id=${it.id}" }
        .mapToAppError()

    override suspend fun updateTransaction(
        transactionId: String,
        request: UpdateTransactionRequest,
    ): Result<Transaction> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "updateTransaction", "id=$transactionId, amount=${request.amount}")

        if (!sessionRepository.isAuthenticated()) {
            val existing = transactionDao.getById(transactionId)
            val updated = existing?.copy(
                title = request.title,
                amount = request.amount,
                categoryId = request.categoryId,
                type = request.type.name.lowercase(),
                note = request.note,
                date = request.date,
                sourceCardId = request.sourceCardId,
                updatedAt = Instant.now().toString(),
            ) ?: return@runCatching error("Transaction $transactionId not found locally")
            transactionDao.upsert(updated)
            val category = request.categoryId?.let { categoryDao.getById(it)?.toDomain() }
            return@runCatching updated.toDomain(category = category)
        }

        val row = request.toJsonObject()
        val updatedDto = supabase.from("transactions")
            .update(row) {
                filter { eq("id", transactionId) }
                select()
            }
            .decodeSingle<TransactionDto>()
        transactionDao.upsert(updatedDto.toEntity(syncedAt = Instant.now().toString()))
        attachCategories(listOf(updatedDto)).first()
    }.logResult(AppLog.Feature.TRANSACTION, "updateTransaction") { "id=${it.id}" }
        .mapToAppError()

    override suspend fun deleteTransaction(transactionId: String): Result<Unit> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "deleteTransaction", "id=$transactionId")
        transactionDao.deleteById(transactionId)
        if (sessionRepository.isAuthenticated()) {
            supabase.from("transactions").delete { filter { eq("id", transactionId) } }
        }
        Unit
    }.logResult(AppLog.Feature.TRANSACTION, "deleteTransaction") { "deleted" }
        .mapToAppError()

    override suspend fun copyFromYesterday(
        userId: String,
        fromDate: String,
        toDate: String,
    ): Result<List<Transaction>> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "copyFromYesterday", "from=$fromDate, to=$toDate")

        val source = transactionDao.getByDate(userId, fromDate)
        if (source.isEmpty()) return@runCatching emptyList()
        val maxPos = transactionDao.getMaxPositionForDate(userId, toDate) ?: -1
        val now = Instant.now().toString()
        val copies = source.mapIndexed { index, t ->
            t.copy(
                id = UUID.randomUUID().toString(),
                date = toDate,
                position = maxPos + 1 + index,
                createdAt = now,
                updatedAt = null,
                syncedAt = null,
            )
        }
        transactionDao.upsert(*copies.toTypedArray())

        if (sessionRepository.isAuthenticated()) {
            runCatching {
                val insertRows = copies.map { entity ->
                    buildJsonObject {
                        put("id", entity.id)
                        put("user_id", entity.userId)
                        put("date", entity.date)
                        put("title", entity.title)
                        put("amount", entity.amount)
                        put("type", entity.type)
                        put("position", entity.position)
                        if (entity.sourceCardId != null) put("source_card_id", entity.sourceCardId)
                        if (entity.categoryId != null) put("category_id", entity.categoryId)
                        if (entity.note != null) put("note", entity.note)
                    }
                }
                val inserted = supabase.from("transactions")
                    .insert(insertRows) { select() }
                    .decodeList<TransactionDto>()
                val syncNow = Instant.now().toString()
                transactionDao.upsert(*inserted.map { it.toEntity(syncedAt = syncNow) }.toTypedArray())
            }
        }

        attachCategoriesLocal(copies, userId)
    }.logResult(AppLog.Feature.TRANSACTION, "copyFromYesterday") { "${it.size} copied" }
        .mapToAppError()

    override suspend fun searchTransactions(
        userId: String,
        query: String,
        categoryIds: Set<String>,
        startDate: String?,
        endDate: String?,
    ): Result<List<Transaction>> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "searchTransactions", "query=$query, categories=$categoryIds")
        val singleCategoryId = if (categoryIds.size == 1) categoryIds.first() else null
        val results = transactionDao.search(
            userId = userId,
            query = query,
            startDate = startDate,
            endDate = endDate,
        ).let { entities ->
            if (singleCategoryId != null) entities.filter { it.categoryId == singleCategoryId }
            else if (categoryIds.size > 1) entities.filter { it.categoryId in categoryIds }
            else entities
        }
        attachCategoriesLocal(results, userId)
    }.logResult(AppLog.Feature.TRANSACTION, "searchTransactions") { "${it.size} items" }
        .mapToAppError()

    // ── Helpers ──────────────────────────────────────────────────────────────

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

    private suspend fun attachCategoriesLocal(
        entities: List<TransactionEntity>,
        userId: String,
    ): List<Transaction> {
        if (entities.isEmpty()) return emptyList()
        val catIds = entities.mapNotNull { it.categoryId }.distinct()
        if (catIds.isEmpty()) return entities.map { it.toDomain() }
        val categoryById = categoryDao.getAll(userId).associateBy({ it.id }, { it.toDomain() })
        return entities.map { entity ->
            entity.toDomain(category = entity.categoryId?.let { categoryById[it] })
        }
    }

    private fun monthRange(yearMonth: String): Pair<String, String> {
        val startDate = "$yearMonth-01"
        val endDate = if (yearMonth.endsWith("12")) {
            "${yearMonth.take(4).toInt() + 1}-01"
        } else {
            val parts = yearMonth.split("-")
            "${parts[0]}-${(parts[1].toInt() + 1).toString().padStart(2, '0')}"
        }
        return Pair(startDate, "$endDate-01")
    }
}
