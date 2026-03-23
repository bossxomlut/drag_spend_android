package com.bossxomlut.dragspend.data.sync

import com.bossxomlut.dragspend.data.local.AppDatabase
import com.bossxomlut.dragspend.data.local.entity.CategoryEntity
import com.bossxomlut.dragspend.data.local.entity.SpendingCardEntity
import com.bossxomlut.dragspend.data.local.entity.SyncMetadataEntity
import com.bossxomlut.dragspend.data.local.entity.SyncQueueEntity
import com.bossxomlut.dragspend.data.local.entity.SyncStatus
import com.bossxomlut.dragspend.data.local.entity.TransactionEntity
import com.bossxomlut.dragspend.data.model.Category
import com.bossxomlut.dragspend.data.model.SpendingCard
import com.bossxomlut.dragspend.data.model.Transaction
import com.bossxomlut.dragspend.util.AppLog
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Manages synchronization between local database and Supabase.
 *
 * Sync triggers:
 * - App foreground (via lifecycle observer)
 * - Connectivity change (when network becomes available)
 * - Manual trigger (pull-to-refresh)
 */
class SyncManager(
    private val database: AppDatabase,
    private val supabase: SupabaseClient,
    private val connectivityMonitor: ConnectivityMonitor,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    /**
     * Flow of pending sync count across all tables.
     */
    val pendingSyncCount: Flow<Int> = combine(
        database.transactionDao().getPendingSyncCountFlow(),
        database.categoryDao().getPendingSyncCountFlow(),
        database.spendingCardDao().getPendingSyncCountFlow(),
    ) { txn, cat, card -> txn + cat + card }

    /**
     * Starts observing connectivity changes and triggers sync when connected.
     */
    fun startObserving() {
        scope.launch {
            connectivityMonitor.observeConnectivity().collect { isConnected ->
                AppLog.d(AppLog.Feature.SYNC, "connectivity", "isConnected=$isConnected")
                if (isConnected) {
                    triggerSync()
                }
            }
        }
    }

    /**
     * Triggers a full sync operation:
     * 1. Push local changes to server
     * 2. Pull remote changes to local
     */
    fun triggerSync() {
        if (_isSyncing.value) {
            AppLog.d(AppLog.Feature.SYNC, "triggerSync", "Already syncing, skipping")
            return
        }

        scope.launch {
            try {
                _isSyncing.value = true
                _lastSyncError.value = null

                if (!connectivityMonitor.isConnected()) {
                    AppLog.d(AppLog.Feature.SYNC, "triggerSync", "No connectivity, skipping")
                    return@launch
                }

                AppLog.d(AppLog.Feature.SYNC, "triggerSync", "Starting sync...")

                // 1. Push local changes
                pushPendingChanges()

                // 2. Pull remote changes
                pullRemoteChanges()

                AppLog.d(AppLog.Feature.SYNC, "triggerSync", "Sync completed successfully")
            } catch (e: Exception) {
                AppLog.e(AppLog.Feature.SYNC, "triggerSync", "Sync failed: ${e.message}")
                _lastSyncError.value = e.message
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Called when app comes to foreground.
     */
    fun onAppForeground() {
        AppLog.d(AppLog.Feature.SYNC, "onAppForeground", "App foregrounded, triggering sync")
        triggerSync()
    }

    // ─── Push Logic ───

    private suspend fun pushPendingChanges() {
        pushPendingTransactions()
        pushPendingCategories()
        pushPendingCards()
    }

    private suspend fun pushPendingTransactions() {
        val pending = database.transactionDao().getPendingSync()
        AppLog.d(AppLog.Feature.SYNC, "pushPendingTransactions", "${pending.size} pending")

        for (entity in pending) {
            try {
                when (entity.syncStatus) {
                    SyncStatus.PENDING_CREATE -> {
                        val serverTxn = supabase.from("transactions")
                            .insert(entity.toRemoteMap()) { select() }
                            .decodeSingle<Transaction>()
                        database.transactionDao().markSynced(
                            entity.id,
                            SyncStatus.SYNCED,
                            serverTxn.updatedAt,
                        )
                    }
                    SyncStatus.PENDING_UPDATE -> {
                        val serverTxn = supabase.from("transactions")
                            .update(entity.toRemoteMap()) {
                                filter { eq("id", entity.id) }
                                select()
                            }
                            .decodeSingle<Transaction>()
                        database.transactionDao().markSynced(
                            entity.id,
                            SyncStatus.SYNCED,
                            serverTxn.updatedAt,
                        )
                    }
                    SyncStatus.PENDING_DELETE -> {
                        supabase.from("transactions")
                            .delete { filter { eq("id", entity.id) } }
                        database.transactionDao().deleteById(entity.id)
                    }
                    SyncStatus.SYNCED -> { /* no-op */ }
                }
            } catch (e: Exception) {
                AppLog.e(AppLog.Feature.SYNC, "pushTransaction", "Failed to sync ${entity.id}: ${e.message}")
                // Continue with other items
            }
        }
    }

    private suspend fun pushPendingCategories() {
        val pending = database.categoryDao().getPendingSync()
        AppLog.d(AppLog.Feature.SYNC, "pushPendingCategories", "${pending.size} pending")

        for (entity in pending) {
            try {
                when (entity.syncStatus) {
                    SyncStatus.PENDING_CREATE -> {
                        val serverCat = supabase.from("categories")
                            .insert(entity.toRemoteMap()) { select() }
                            .decodeSingle<Category>()
                        database.categoryDao().markSynced(
                            entity.id,
                            SyncStatus.SYNCED,
                            serverCat.createdAt,
                        )
                    }
                    SyncStatus.PENDING_UPDATE -> {
                        val serverCat = supabase.from("categories")
                            .update(entity.toRemoteMap()) {
                                filter { eq("id", entity.id) }
                                select()
                            }
                            .decodeSingle<Category>()
                        database.categoryDao().markSynced(
                            entity.id,
                            SyncStatus.SYNCED,
                            serverCat.createdAt,
                        )
                    }
                    SyncStatus.PENDING_DELETE -> {
                        supabase.from("categories")
                            .delete { filter { eq("id", entity.id) } }
                        database.categoryDao().deleteById(entity.id)
                    }
                    SyncStatus.SYNCED -> { /* no-op */ }
                }
            } catch (e: Exception) {
                AppLog.e(AppLog.Feature.SYNC, "pushCategory", "Failed to sync ${entity.id}: ${e.message}")
            }
        }
    }

    private suspend fun pushPendingCards() {
        val pending = database.spendingCardDao().getPendingSync()
        AppLog.d(AppLog.Feature.SYNC, "pushPendingCards", "${pending.size} pending")

        for (entity in pending) {
            try {
                when (entity.syncStatus) {
                    SyncStatus.PENDING_CREATE -> {
                        val serverCard = supabase.from("spending_cards")
                            .insert(entity.toRemoteMap()) { select() }
                            .decodeSingle<SpendingCard>()
                        database.spendingCardDao().markSynced(
                            entity.id,
                            SyncStatus.SYNCED,
                            serverCard.updatedAt,
                        )
                    }
                    SyncStatus.PENDING_UPDATE -> {
                        val serverCard = supabase.from("spending_cards")
                            .update(entity.toRemoteMap()) {
                                filter { eq("id", entity.id) }
                                select()
                            }
                            .decodeSingle<SpendingCard>()
                        database.spendingCardDao().markSynced(
                            entity.id,
                            SyncStatus.SYNCED,
                            serverCard.updatedAt,
                        )
                    }
                    SyncStatus.PENDING_DELETE -> {
                        supabase.from("spending_cards")
                            .delete { filter { eq("id", entity.id) } }
                        database.spendingCardDao().deleteById(entity.id)
                    }
                    SyncStatus.SYNCED -> { /* no-op */ }
                }
            } catch (e: Exception) {
                AppLog.e(AppLog.Feature.SYNC, "pushCard", "Failed to sync ${entity.id}: ${e.message}")
            }
        }
    }

    // ─── Pull Logic ───

    private suspend fun pullRemoteChanges() {
        // Pull transactions
        pullTransactions()

        // Pull categories
        pullCategories()

        // Pull spending cards
        pullSpendingCards()
    }

    private suspend fun pullTransactions() {
        val metadata = database.syncMetadataDao().get(SyncQueueEntity.TABLE_TRANSACTIONS)
        val lastTimestamp = metadata?.lastServerTimestamp ?: "1970-01-01T00:00:00Z"

        AppLog.d(AppLog.Feature.SYNC, "pullTransactions", "lastTimestamp=$lastTimestamp")

        val remote = supabase.from("transactions")
            .select {
                filter { gt("updated_at", lastTimestamp) }
                order("updated_at", Order.ASCENDING)
            }
            .decodeList<Transaction>()

        AppLog.d(AppLog.Feature.SYNC, "pullTransactions", "${remote.size} remote changes")

        for (txn in remote) {
            val local = database.transactionDao().getById(txn.id)
            if (local == null || local.syncStatus == SyncStatus.SYNCED) {
                // No local changes, accept remote
                database.transactionDao().insert(
                    TransactionEntity.fromDomain(txn, SyncStatus.SYNCED, txn.updatedAt)
                )
            } else {
                // Conflict: server wins
                AppLog.d(AppLog.Feature.SYNC, "pullTransactions", "Conflict for ${txn.id}, server wins")
                database.transactionDao().insert(
                    TransactionEntity.fromDomain(txn, SyncStatus.SYNCED, txn.updatedAt)
                )
            }
        }

        // Update metadata
        if (remote.isNotEmpty()) {
            val maxTimestamp = remote.maxOfOrNull { it.updatedAt ?: "" } ?: lastTimestamp
            database.syncMetadataDao().upsert(
                SyncMetadataEntity(
                    tableName = SyncQueueEntity.TABLE_TRANSACTIONS,
                    lastSyncAt = Instant.now().toString(),
                    lastServerTimestamp = maxTimestamp,
                )
            )
        }
    }

    private suspend fun pullCategories() {
        val metadata = database.syncMetadataDao().get(SyncQueueEntity.TABLE_CATEGORIES)
        val lastTimestamp = metadata?.lastServerTimestamp ?: "1970-01-01T00:00:00Z"

        AppLog.d(AppLog.Feature.SYNC, "pullCategories", "lastTimestamp=$lastTimestamp")

        val remote = supabase.from("categories")
            .select {
                filter { gt("created_at", lastTimestamp) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<Category>()

        AppLog.d(AppLog.Feature.SYNC, "pullCategories", "${remote.size} remote changes")

        for (cat in remote) {
            val local = database.categoryDao().getById(cat.id)
            if (local == null || local.syncStatus == SyncStatus.SYNCED) {
                database.categoryDao().insert(
                    CategoryEntity.fromDomain(cat, SyncStatus.SYNCED, cat.createdAt)
                )
            } else {
                AppLog.d(AppLog.Feature.SYNC, "pullCategories", "Conflict for ${cat.id}, server wins")
                database.categoryDao().insert(
                    CategoryEntity.fromDomain(cat, SyncStatus.SYNCED, cat.createdAt)
                )
            }
        }

        if (remote.isNotEmpty()) {
            val maxTimestamp = remote.maxOfOrNull { it.createdAt ?: "" } ?: lastTimestamp
            database.syncMetadataDao().upsert(
                SyncMetadataEntity(
                    tableName = SyncQueueEntity.TABLE_CATEGORIES,
                    lastSyncAt = Instant.now().toString(),
                    lastServerTimestamp = maxTimestamp,
                )
            )
        }
    }

    private suspend fun pullSpendingCards() {
        val metadata = database.syncMetadataDao().get(SyncQueueEntity.TABLE_SPENDING_CARDS)
        val lastTimestamp = metadata?.lastServerTimestamp ?: "1970-01-01T00:00:00Z"

        AppLog.d(AppLog.Feature.SYNC, "pullSpendingCards", "lastTimestamp=$lastTimestamp")

        val remote = supabase.from("spending_cards")
            .select {
                filter { gt("updated_at", lastTimestamp) }
                order("updated_at", Order.ASCENDING)
            }
            .decodeList<SpendingCard>()

        AppLog.d(AppLog.Feature.SYNC, "pullSpendingCards", "${remote.size} remote changes")

        for (card in remote) {
            val local = database.spendingCardDao().getById(card.id)
            if (local == null || local.syncStatus == SyncStatus.SYNCED) {
                database.spendingCardDao().insert(
                    SpendingCardEntity.fromDomain(card, SyncStatus.SYNCED, card.updatedAt)
                )
            } else {
                AppLog.d(AppLog.Feature.SYNC, "pullSpendingCards", "Conflict for ${card.id}, server wins")
                database.spendingCardDao().insert(
                    SpendingCardEntity.fromDomain(card, SyncStatus.SYNCED, card.updatedAt)
                )
            }
        }

        if (remote.isNotEmpty()) {
            val maxTimestamp = remote.maxOfOrNull { it.updatedAt ?: "" } ?: lastTimestamp
            database.syncMetadataDao().upsert(
                SyncMetadataEntity(
                    tableName = SyncQueueEntity.TABLE_SPENDING_CARDS,
                    lastSyncAt = Instant.now().toString(),
                    lastServerTimestamp = maxTimestamp,
                )
            )
        }
    }

    /**
     * Clears all local data (called on logout).
     */
    suspend fun clearLocalData() {
        AppLog.d(AppLog.Feature.SYNC, "clearLocalData", "Clearing all local data")
        database.clearAllTables()
    }

    // ─── Helper Extensions ───

    private fun TransactionEntity.toRemoteMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "user_id" to userId,
        "source_card_id" to sourceCardId,
        "date" to date,
        "title" to title,
        "amount" to amount,
        "category_id" to categoryId,
        "type" to type,
        "note" to note,
        "position" to position,
    )

    private fun CategoryEntity.toRemoteMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "user_id" to userId,
        "name" to name,
        "icon" to icon,
        "color" to color,
        "type" to type,
        "language" to language,
    )

    private fun SpendingCardEntity.toRemoteMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "user_id" to userId,
        "title" to title,
        "category_id" to categoryId,
        "type" to type,
        "note" to note,
        "position" to position,
        "use_count" to useCount,
        "language" to language,
    )
}
