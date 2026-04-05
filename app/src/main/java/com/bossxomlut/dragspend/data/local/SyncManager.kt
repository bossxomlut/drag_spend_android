package com.bossxomlut.dragspend.data.local

import com.bossxomlut.dragspend.data.local.dao.CardDao
import com.bossxomlut.dragspend.data.local.dao.CardVariantDao
import com.bossxomlut.dragspend.data.local.dao.CategoryDao
import com.bossxomlut.dragspend.data.local.dao.TransactionDao
import com.bossxomlut.dragspend.data.model.CardVariantDto
import com.bossxomlut.dragspend.data.model.CategoryDto
import com.bossxomlut.dragspend.data.model.SpendingCardDto
import com.bossxomlut.dragspend.data.model.TransactionDto
import com.bossxomlut.dragspend.util.AppLog
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.time.LocalDate

/**
 * Pull toàn bộ dữ liệu của người dùng từ Supabase xuống Room.
 * Gọi sau khi đăng nhập thành công.
 */
class SyncManager(
    private val supabase: SupabaseClient,
    private val categoryDao: CategoryDao,
    private val cardDao: CardDao,
    private val cardVariantDao: CardVariantDao,
    private val transactionDao: TransactionDao,
) {

    suspend fun pullAll(userId: String) {
        AppLog.d(AppLog.Feature.PERF, "SyncManager.pullAll", "userId=${userId.take(8)}")
        val now = Instant.now().toString()
        pullCategories(userId, now)
        pullCards(userId, now)
        pullTransactions(userId, now)
    }

    private suspend fun pullCategories(userId: String, syncedAt: String) {
        runCatching {
            supabase.from("categories")
                .select { filter { eq("user_id", userId) } }
                .decodeList<CategoryDto>()
                .map { it.toEntity(syncedAt = syncedAt) }
        }.onSuccess { entities ->
            categoryDao.deleteAllForUser(userId)
            if (entities.isNotEmpty()) categoryDao.upsert(*entities.toTypedArray())
            AppLog.d(AppLog.Feature.PERF, "SyncManager.pullCategories", "${entities.size} synced")
        }.onFailure { e ->
            AppLog.error(AppLog.Feature.PERF, "SyncManager.pullCategories", e, "failed")
        }
    }

    private suspend fun pullCards(userId: String, syncedAt: String) {
        runCatching {
            val cardDtos = supabase.from("spending_cards")
                .select { filter { eq("user_id", userId) } }
                .decodeList<SpendingCardDto>()

            val variantDtos = if (cardDtos.isNotEmpty()) {
                supabase.from("card_variants")
                    .select { filter { isIn("card_id", cardDtos.map { it.id }) } }
                    .decodeList<CardVariantDto>()
            } else {
                emptyList()
            }
            Pair(cardDtos, variantDtos)
        }.onSuccess { (cardDtos, variantDtos) ->
            cardDao.deleteAllForUser(userId)
            cardVariantDao.deleteAllForUser(userId)
            if (cardDtos.isNotEmpty()) {
                cardDao.upsert(*cardDtos.map { it.toEntity(syncedAt = syncedAt) }.toTypedArray())
            }
            if (variantDtos.isNotEmpty()) {
                cardVariantDao.upsert(*variantDtos.map { it.toEntity(syncedAt = syncedAt) }.toTypedArray())
            }
            AppLog.d(AppLog.Feature.PERF, "SyncManager.pullCards", "${cardDtos.size} cards synced")
        }.onFailure { e ->
            AppLog.error(AppLog.Feature.PERF, "SyncManager.pullCards", e, "failed")
        }
    }

    private suspend fun pullTransactions(userId: String, syncedAt: String) {
        val threeMonthsAgo = LocalDate.now().minusMonths(3).toString()
        runCatching {
            supabase.from("transactions")
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("date", threeMonthsAgo)
                    }
                    order("date", order = Order.ASCENDING)
                }
                .decodeList<TransactionDto>()
                .map { it.toEntity(syncedAt = syncedAt) }
        }.onSuccess { entities ->
            if (entities.isNotEmpty()) transactionDao.upsert(*entities.toTypedArray())
            AppLog.d(AppLog.Feature.PERF, "SyncManager.pullTransactions", "${entities.size} synced")
        }.onFailure { e ->
            AppLog.error(AppLog.Feature.PERF, "SyncManager.pullTransactions", e, "failed")
        }
    }
}
