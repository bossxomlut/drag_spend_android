package com.bossxomlut.dragspend.data.repository

import com.bossxomlut.dragspend.data.local.dao.TransactionDao
import com.bossxomlut.dragspend.data.local.entity.SyncStatus
import com.bossxomlut.dragspend.data.local.entity.TransactionEntity
import com.bossxomlut.dragspend.data.model.MonthlyReportRow
import com.bossxomlut.dragspend.data.model.Transaction
import com.bossxomlut.dragspend.data.sync.ConnectivityMonitor
import com.bossxomlut.dragspend.data.sync.SyncManager
import com.bossxomlut.dragspend.domain.repository.CategoryRepository
import com.bossxomlut.dragspend.domain.repository.CreateTransactionRequest
import com.bossxomlut.dragspend.domain.repository.TransactionRepository
import com.bossxomlut.dragspend.domain.repository.UpdateTransactionRequest
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.logResult
import io.github.jan.supabase.SupabaseClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TransactionRepositoryImpl(
    private val supabase: SupabaseClient,
    private val categoryRepository: CategoryRepository,
    private val transactionDao: TransactionDao,
    private val connectivityMonitor: ConnectivityMonitor,
    private val syncManager: SyncManager,
) : TransactionRepository {

    /**
     * Returns a Flow of transactions for the given date.
     * This is the reactive, local-first API for UI consumption.
     */
    fun getTransactionsFlow(userId: String, date: String): Flow<List<Transaction>> {
        return transactionDao.getByDateFlow(userId, date).map { entities ->
            val transactions = entities.map { it.toDomain() }
            attachCategories(transactions)
        }
    }

    override suspend fun getTransactions(userId: String, date: String): Result<List<Transaction>> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "getTransactions", "userId=${userId.take(8)}, date=$date")

        // Try local first
        val localEntities = transactionDao.getByDate(userId, date)
            .filter { it.syncStatus != SyncStatus.PENDING_DELETE }

        if (localEntities.isNotEmpty()) {
            AppLog.d(AppLog.Feature.TRANSACTION, "getTransactions", "Found ${localEntities.size} local")
            val transactions = localEntities.map { it.toDomain() }
            return@runCatching attachCategories(transactions)
        }

        // Fallback to remote if online
        if (!connectivityMonitor.isConnected()) {
            AppLog.d(AppLog.Feature.TRANSACTION, "getTransactions", "Offline, returning empty")
            return@runCatching emptyList()
        }

        val txns = supabase.from("transactions")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("date", date)
                }
                order("position", order = Order.ASCENDING)
            }
            .decodeList<Transaction>()

        // Cache to local
        val entities = txns.map { TransactionEntity.fromDomain(it, SyncStatus.SYNCED, it.updatedAt) }
        transactionDao.insertAll(entities)

        attachCategories(txns)
    }.logResult(AppLog.Feature.TRANSACTION, "getTransactions") { "${it.size} items" }

    override suspend fun getMonthlyTransactions(userId: String, yearMonth: String): Result<List<Transaction>> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "getMonthlyTransactions", "userId=${userId.take(8)}, yearMonth=$yearMonth")

        // If offline, use local data
        if (!connectivityMonitor.isConnected()) {
            AppLog.d(AppLog.Feature.TRANSACTION, "getMonthlyTransactions", "Offline, using local data")
            val localEntities = transactionDao.getByMonth(userId, yearMonth)
                .filter { it.syncStatus != SyncStatus.PENDING_DELETE }
            val transactions = localEntities.map { it.toDomain() }
            return@runCatching attachCategories(transactions)
        }

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

        // If offline, compute report from local data
        if (!connectivityMonitor.isConnected()) {
            AppLog.d(AppLog.Feature.TRANSACTION, "getMonthlyReport", "Offline, computing from local data")
            val localTransactions = transactionDao.getByMonth(userId, yearMonth)
                .filter { it.syncStatus != SyncStatus.PENDING_DELETE }
            return@runCatching computeLocalMonthlyReport(localTransactions)
        }

        val params = buildJsonObject { put("p_year_month", yearMonth) }
        supabase.postgrest.rpc("get_monthly_report", params).decodeList<MonthlyReportRow>()
    }.logResult(AppLog.Feature.TRANSACTION, "getMonthlyReport") { "${it.size} rows" }

    /**
     * Computes a monthly report from local transaction entities.
     * Groups transactions by date and category, similar to server's get_monthly_report function.
     */
    private suspend fun computeLocalMonthlyReport(entities: List<TransactionEntity>): List<MonthlyReportRow> {
        // Get categories for mapping
        val categoryMap = categoryRepository.getCategories(entities.firstOrNull()?.userId ?: "")
            .getOrNull()
            ?.associateBy { it.id }
            ?: emptyMap()

        // Group by date and category
        return entities.groupBy { Triple(it.date, it.categoryId, it.type) }
            .map { (key, txns) ->
                val (date, categoryId, type) = key
                val category = categoryId?.let { categoryMap[it] }
                MonthlyReportRow(
                    date = date,
                    categoryId = categoryId,
                    categoryName = category?.name,
                    categoryIcon = category?.icon,
                    categoryColor = category?.color,
                    type = if (type == "income") com.bossxomlut.dragspend.data.model.TransactionType.INCOME 
                           else com.bossxomlut.dragspend.data.model.TransactionType.EXPENSE,
                    total = txns.sumOf { it.amount },
                )
            }
            .sortedWith(compareBy({ it.date }, { it.categoryId }))
    }

    override suspend fun createTransaction(request: CreateTransactionRequest): Result<Transaction> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "createTransaction", "date=${request.date}, title=${request.title}, amount=${request.amount}, type=${request.type}")

        // Get max position from local first, then remote if needed
        val localMaxPosition = transactionDao.getMaxPosition(request.userId, request.date) ?: -1
        val maxPosition = if (connectivityMonitor.isConnected()) {
            val remoteMaxPosition = supabase.from("transactions")
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
            maxOf(localMaxPosition, remoteMaxPosition)
        } else {
            localMaxPosition
        }

        val newPosition = maxPosition + 1
        val newId = UUID.randomUUID().toString()
        val now = java.time.Instant.now().toString()

        // Create transaction domain object
        val transaction = Transaction(
            id = newId,
            userId = request.userId,
            sourceCardId = request.sourceCardId,
            date = request.date,
            title = request.title,
            amount = request.amount,
            categoryId = request.categoryId,
            type = request.type,
            note = request.note,
            position = newPosition,
            createdAt = now,
            updatedAt = now,
            category = null,
        )

        // Save to local with PENDING_CREATE status
        val syncStatus = if (connectivityMonitor.isConnected()) {
            // Try to sync immediately
            try {
                val row = request.toJsonObject(newPosition)
                val serverTxn = supabase.from("transactions")
                    .insert(row) { select() }
                    .decodeSingle<Transaction>()
                // Update local with server response
                transactionDao.insert(TransactionEntity.fromDomain(serverTxn, SyncStatus.SYNCED, serverTxn.updatedAt))
                return@runCatching serverTxn
            } catch (e: Exception) {
                AppLog.e(AppLog.Feature.TRANSACTION, "createTransaction", "Remote failed, saving locally: ${e.message}")
                SyncStatus.PENDING_CREATE
            }
        } else {
            SyncStatus.PENDING_CREATE
        }

        // Save locally
        val entity = TransactionEntity.fromDomain(transaction, syncStatus, null)
        transactionDao.insert(entity)

        // Trigger sync in background if pending
        if (syncStatus == SyncStatus.PENDING_CREATE) {
            syncManager.triggerSync()
        }

        transaction
    }.logResult(AppLog.Feature.TRANSACTION, "createTransaction") { "id=${it.id}" }

    override suspend fun updateTransaction(
        transactionId: String,
        request: UpdateTransactionRequest,
    ): Result<Transaction> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "updateTransaction", "id=$transactionId, amount=${request.amount}, type=${request.type}")

        // Get existing transaction
        val existing = transactionDao.getById(transactionId)
            ?: throw IllegalStateException("Transaction not found: $transactionId")

        val now = java.time.Instant.now().toString()

        // Determine new sync status
        val newSyncStatus = when (existing.syncStatus) {
            SyncStatus.PENDING_CREATE -> SyncStatus.PENDING_CREATE // Still pending create
            else -> SyncStatus.PENDING_UPDATE
        }

        // Update local
        val updatedEntity = existing.copy(
            title = request.title,
            amount = request.amount,
            categoryId = request.categoryId,
            type = request.type.name.lowercase(),
            note = request.note,
            date = request.date,
            sourceCardId = request.sourceCardId,
            updatedAt = now,
            syncStatus = newSyncStatus,
        )
        transactionDao.update(updatedEntity)

        // Try to sync immediately if online
        if (connectivityMonitor.isConnected()) {
            try {
                val row = request.toJsonObject()
                val serverTxn = supabase.from("transactions")
                    .update(row) {
                        filter { eq("id", transactionId) }
                        select()
                    }
                    .decodeSingle<Transaction>()
                // Mark as synced
                transactionDao.markSynced(transactionId, SyncStatus.SYNCED, serverTxn.updatedAt)
                return@runCatching attachCategories(listOf(serverTxn)).first()
            } catch (e: Exception) {
                AppLog.e(AppLog.Feature.TRANSACTION, "updateTransaction", "Remote failed: ${e.message}")
                // Trigger background sync
                syncManager.triggerSync()
            }
        } else {
            syncManager.triggerSync()
        }

        updatedEntity.toDomain()
    }.logResult(AppLog.Feature.TRANSACTION, "updateTransaction") { "id=${it.id}" }

    override suspend fun deleteTransaction(transactionId: String): Result<Unit> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "deleteTransaction", "id=$transactionId")

        val existing = transactionDao.getById(transactionId)

        if (existing?.syncStatus == SyncStatus.PENDING_CREATE) {
            // Never synced to server, just delete locally
            transactionDao.deleteById(transactionId)
        } else {
            // Mark for deletion
            existing?.let {
                transactionDao.updateSyncStatus(transactionId, SyncStatus.PENDING_DELETE)
            }

            // Try to delete on server if online
            if (connectivityMonitor.isConnected()) {
                try {
                    supabase.from("transactions")
                        .delete { filter { eq("id", transactionId) } }
                    // Delete from local
                    transactionDao.deleteById(transactionId)
                } catch (e: Exception) {
                    AppLog.e(AppLog.Feature.TRANSACTION, "deleteTransaction", "Remote failed: ${e.message}")
                    syncManager.triggerSync()
                }
            } else {
                syncManager.triggerSync()
            }
        }
        Unit
    }.logResult(AppLog.Feature.TRANSACTION, "deleteTransaction") { "deleted" }

    override suspend fun copyFromYesterday(
        userId: String,
        fromDate: String,
        toDate: String,
    ): Result<List<Transaction>> = runCatching {
        AppLog.d(AppLog.Feature.TRANSACTION, "copyFromYesterday", "from=$fromDate, to=$toDate")

        // Get yesterday's transactions from local or remote
        val yesterday = if (connectivityMonitor.isConnected()) {
            supabase.from("transactions")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("date", fromDate)
                    }
                    order("position", order = Order.ASCENDING)
                }
                .decodeList<Transaction>()
        } else {
            transactionDao.getByDate(userId, fromDate)
                .filter { it.syncStatus != SyncStatus.PENDING_DELETE }
                .map { it.toDomain() }
        }

        if (yesterday.isEmpty()) return@runCatching emptyList()

        // Get max position for today
        val localMaxPosition = transactionDao.getMaxPosition(userId, toDate) ?: -1
        val maxPosition = if (connectivityMonitor.isConnected()) {
            val remoteMaxPosition = supabase.from("transactions")
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
            maxOf(localMaxPosition, remoteMaxPosition)
        } else {
            localMaxPosition
        }

        // Create new transactions
        val now = java.time.Instant.now().toString()
        val newTransactions = yesterday.mapIndexed { index, t ->
            val newId = UUID.randomUUID().toString()
            Transaction(
                id = newId,
                userId = userId,
                sourceCardId = t.sourceCardId,
                date = toDate,
                title = t.title,
                amount = t.amount,
                categoryId = t.categoryId,
                type = t.type,
                note = t.note,
                position = maxPosition + 1 + index,
                createdAt = now,
                updatedAt = now,
                category = t.category,
            )
        }

        if (connectivityMonitor.isConnected()) {
            // Try to sync to server
            try {
                val newRows = newTransactions.map { t ->
                    CreateTransactionRequest(
                        userId = t.userId,
                        sourceCardId = t.sourceCardId,
                        date = t.date,
                        title = t.title,
                        amount = t.amount,
                        categoryId = t.categoryId,
                        type = t.type,
                        note = t.note,
                    ).toJsonObject(t.position)
                }
                val serverTxns = supabase.from("transactions")
                    .insert(newRows) { select() }
                    .decodeList<Transaction>()
                // Save synced versions locally
                val entities = serverTxns.map { TransactionEntity.fromDomain(it, SyncStatus.SYNCED, it.updatedAt) }
                transactionDao.insertAll(entities)
                return@runCatching attachCategories(serverTxns)
            } catch (e: Exception) {
                AppLog.e(AppLog.Feature.TRANSACTION, "copyFromYesterday", "Remote failed: ${e.message}")
            }
        }

        // Save locally as pending
        val entities = newTransactions.map { TransactionEntity.fromDomain(it, SyncStatus.PENDING_CREATE, null) }
        transactionDao.insertAll(entities)
        syncManager.triggerSync()
        attachCategories(newTransactions)
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

        // Get transactions from local or remote
        val txns = if (connectivityMonitor.isConnected()) {
            supabase.from("transactions")
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
        } else {
            // Search from local database
            transactionDao.getByDateRange(userId, start, exclusiveEnd)
                .filter { it.syncStatus != SyncStatus.PENDING_DELETE }
                .map { it.toDomain() }
                .sortedWith(compareByDescending<Transaction> { it.date }.thenBy { it.position })
        }

        AppLog.d(
            AppLog.Feature.TRANSACTION,
            "searchTransactions",
            "Got ${txns.size} transactions",
        )

        val withCategories = attachCategories(txns)

        // Client-side filtering (full-text search, category, AND dates)
        // Full-text: split query into words, search in title + note + category name
        // All words must match somewhere (AND logic)
        val searchWords = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }

        withCategories.filter { tx ->
            val matchesQuery = if (searchWords.isEmpty()) {
                true
            } else {
                // Combine all searchable text fields
                val searchableText = buildString {
                    append(tx.title.lowercase())
                    append(" ")
                    tx.note?.let { append(it.lowercase()); append(" ") }
                    tx.category?.name?.let { append(it.lowercase()) }
                }
                // All words must be found in the combined text
                searchWords.all { word -> searchableText.contains(word) }
            }
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
