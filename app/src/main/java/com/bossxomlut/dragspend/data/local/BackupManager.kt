package com.bossxomlut.dragspend.data.local

import com.bossxomlut.dragspend.data.local.dao.CardDao
import com.bossxomlut.dragspend.data.local.dao.CardVariantDao
import com.bossxomlut.dragspend.data.local.dao.CategoryDao
import com.bossxomlut.dragspend.data.local.dao.TransactionDao
import com.bossxomlut.dragspend.data.model.CardVariantDto
import com.bossxomlut.dragspend.data.model.CategoryDto
import com.bossxomlut.dragspend.data.model.SpendingCardDto
import com.bossxomlut.dragspend.data.model.TransactionDto
import com.bossxomlut.dragspend.data.model.TransactionTypeDto
import com.bossxomlut.dragspend.util.AppLog
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Push dữ liệu từ Room lên Supabase (ngược lại với SyncManager).
 *
 * Hỗ trợ hai trường hợp:
 * - **migrateRoomUserId** : cập nhật userId trong Room (Room-only, nhanh, không cần mạng).
 * - **pushAll** : push toàn bộ dữ liệu local lên Supabase (cần mạng).
 */
class BackupManager(
    private val supabase: SupabaseClient,
    private val categoryDao: CategoryDao,
    private val cardDao: CardDao,
    private val cardVariantDao: CardVariantDao,
    private val transactionDao: TransactionDao,
) {

    data class BackupResult(
        val categories: Int,
        val cards: Int,
        val transactions: Int,
    )

    /** Minimal projection used for newer-wins timestamp comparison — avoids fetching full records. */
    @Serializable
    private data class RemoteTimestamp(
        val id: String,
        @SerialName("updated_at") val updatedAt: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
    )

    /**
     * Cập nhật userId của tất cả dữ liệu local từ [fromUserId] sang [toUserId] trong Room.
     * Chỉ thao tác trên Room — không cần kết nối mạng.
     * Gọi đồng bộ trước khi navigate vào Dashboard để tránh màn hình trống sau login.
     */
    suspend fun migrateRoomUserId(fromUserId: String, toUserId: String) {
        if (fromUserId == toUserId) return

        AppLog.d(
            AppLog.Feature.PERF,
            "BackupManager.migrateRoom",
            "from=${fromUserId.take(8)} to=${toUserId.take(8)}",
        )
        val syncedAt = Instant.now().toString()

        val categories = categoryDao.getAll(fromUserId)
        if (categories.isNotEmpty()) {
            categoryDao.upsert(*categories.map { it.copy(userId = toUserId, syncedAt = syncedAt) }.toTypedArray())
        }

        val cards = cardDao.getAll(fromUserId)
        if (cards.isNotEmpty()) {
            cardDao.upsert(*cards.map { it.copy(userId = toUserId, syncedAt = syncedAt) }.toTypedArray())
        }

        val transactions = transactionDao.getAll(fromUserId)
        if (transactions.isNotEmpty()) {
            transactionDao.upsert(*transactions.map { it.copy(userId = toUserId, syncedAt = syncedAt) }.toTypedArray())
        }

        // card_variants không có user_id nên không cần migrate.

        AppLog.success(
            AppLog.Feature.PERF,
            "BackupManager.migrateRoom",
            "${categories.size} categories, ${cards.size} cards, ${transactions.size} transactions",
        )
    }

    /**
     * Push toàn bộ dữ liệu local (theo [userId]) lên Supabase.
     *
     * **Chiến lược: "newer wins"**
     * - Trước khi push mỗi loại dữ liệu, fetch `id` + `updated_at` từ Supabase.
     * - Chỉ upsert lên cloud những record mà local mới hơn hoặc không tồn tại trên cloud.
     * - Dữ liệu trong Room KHÔNG bị thay đổi — chỉ ghi lên cloud.
     */
    suspend fun pushAll(userId: String): Result<BackupResult> = runCatching {
        AppLog.d(AppLog.Feature.PERF, "BackupManager.pushAll", "userId=${userId.take(8)}")
        val categories = pushCategories(userId)
        val cards = pushCards(userId)
        val transactions = pushTransactions(userId)
        BackupResult(categories, cards, transactions)
    }

    private suspend fun pushCategories(userId: String): Int {
        val localEntities = categoryDao.getAll(userId)
        if (localEntities.isEmpty()) return 0

        // Fetch only id+timestamps for comparison — avoids downloading full payload
        val remoteMap = runCatching {
            supabase.from("categories")
                .select("id, updated_at, created_at") { filter { eq("user_id", userId) } }
                .decodeList<RemoteTimestamp>()
                .associateBy { it.id }
        }.getOrElse { emptyMap() }

        val toPush = localEntities
            .filter { local ->
                val remote = remoteMap[local.id]
                // Push if: not on remote OR local is newer/equal
                remote == null ||
                    newerOrEqual(local.updatedAt ?: local.createdAt, remote.updatedAt ?: remote.createdAt)
            }
            .map { entity ->
                CategoryDto(
                    id = entity.id,
                    userId = userId,
                    name = entity.name,
                    icon = entity.icon,
                    color = entity.color,
                    type = TransactionTypeDto.valueOf(entity.type.uppercase()),
                    language = entity.language,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt ?: entity.createdAt,
                )
            }

        if (toPush.isEmpty()) {
            AppLog.d(AppLog.Feature.PERF, "BackupManager.pushCategories", "0/${localEntities.size} pushed (remote is newer)")
            return 0
        }
        supabase.from("categories").upsert(toPush)
        AppLog.d(AppLog.Feature.PERF, "BackupManager.pushCategories", "${toPush.size}/${localEntities.size} pushed")
        return toPush.size
    }

    private suspend fun pushCards(userId: String): Int {
        val cardEntities = cardDao.getAll(userId)
        if (cardEntities.isEmpty()) return 0

        val remoteMap = runCatching {
            supabase.from("spending_cards")
                .select("id, updated_at, created_at") { filter { eq("user_id", userId) } }
                .decodeList<RemoteTimestamp>()
                .associateBy { it.id }
        }.getOrElse { emptyMap() }

        val cardsToPush = cardEntities.filter { local ->
            val remote = remoteMap[local.id]
            remote == null ||
                newerOrEqual(local.updatedAt ?: local.createdAt, remote.updatedAt ?: remote.createdAt)
        }

        if (cardsToPush.isNotEmpty()) {
            val cardDtos = cardsToPush.map { entity ->
                SpendingCardDto(
                    id = entity.id,
                    userId = userId,
                    title = entity.title,
                    categoryId = entity.categoryId,
                    type = TransactionTypeDto.valueOf(entity.type.uppercase()),
                    note = entity.note,
                    position = entity.position,
                    useCount = entity.useCount,
                    language = entity.language,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                )
            }
            supabase.from("spending_cards").upsert(cardDtos)

            // Always push variants for cards being pushed
            val variantEntities = cardVariantDao.getByCardIds(cardsToPush.map { it.id })
            if (variantEntities.isNotEmpty()) {
                val variantDtos = variantEntities.map { entity ->
                    CardVariantDto(
                        id = entity.id,
                        cardId = entity.cardId,
                        label = entity.label,
                        amount = entity.amount,
                        isDefault = entity.isDefault,
                        position = entity.position,
                        createdAt = entity.createdAt,
                    )
                }
                supabase.from("card_variants").upsert(variantDtos)
            }
            AppLog.d(AppLog.Feature.PERF, "BackupManager.pushCards", "${cardsToPush.size}/${cardEntities.size} cards, ${variantEntities.size} variants pushed")
        } else {
            AppLog.d(AppLog.Feature.PERF, "BackupManager.pushCards", "0/${cardEntities.size} pushed (remote is newer)")
        }
        return cardsToPush.size
    }

    private suspend fun pushTransactions(userId: String): Int {
        val localEntities = transactionDao.getByDateRange(
            userId = userId,
            startDate = "2000-01-01",
            endDate = "2999-12-31",
        )
        if (localEntities.isEmpty()) return 0

        val remoteMap = runCatching {
            supabase.from("transactions")
                .select("id, updated_at, created_at") { filter { eq("user_id", userId) } }
                .decodeList<RemoteTimestamp>()
                .associateBy { it.id }
        }.getOrElse { emptyMap() }

        val toPush = localEntities
            .filter { local ->
                val remote = remoteMap[local.id]
                remote == null ||
                    newerOrEqual(local.updatedAt ?: local.createdAt, remote.updatedAt ?: remote.createdAt)
            }
            .map { entity ->
                TransactionDto(
                    id = entity.id,
                    userId = userId,
                    sourceCardId = entity.sourceCardId,
                    date = entity.date,
                    title = entity.title,
                    amount = entity.amount,
                    categoryId = entity.categoryId,
                    type = TransactionTypeDto.valueOf(entity.type.uppercase()),
                    note = entity.note,
                    position = entity.position,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                )
            }

        if (toPush.isEmpty()) {
            AppLog.d(AppLog.Feature.PERF, "BackupManager.pushTransactions", "0/${localEntities.size} pushed (remote is newer)")
            return 0
        }
        supabase.from("transactions").upsert(toPush)
        AppLog.d(AppLog.Feature.PERF, "BackupManager.pushTransactions", "${toPush.size}/${localEntities.size} pushed")
        return toPush.size
    }

    /**
     * Trả về true nếu [localTs] mới hơn hoặc bằng [remoteTs].
     * Timestamp format ISO-8601 — so sánh lexicographic là đủ.
     * null được coi là "rất cũ" (oldest possible) để local wins khi remote null.
     */
    private fun newerOrEqual(localTs: String?, remoteTs: String?): Boolean {
        if (localTs == null) return false
        if (remoteTs == null) return true
        return localTs >= remoteTs
    }
}
