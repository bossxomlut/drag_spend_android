package com.bossxomlut.dragspend.data.local

import com.bossxomlut.dragspend.data.local.dao.CardDao
import com.bossxomlut.dragspend.data.local.dao.CardVariantDao
import com.bossxomlut.dragspend.data.local.dao.CategoryDao
import com.bossxomlut.dragspend.data.local.entity.CardVariantEntity
import com.bossxomlut.dragspend.data.local.entity.CategoryEntity
import com.bossxomlut.dragspend.data.local.entity.SpendingCardEntity
import com.bossxomlut.dragspend.util.AppLog
import java.time.Instant
import java.util.UUID

/**
 * Tạo dữ liệu mặc định trong Room cho guest user sau khi chọn ngôn ngữ.
 * Tương đương với `ensure_user_seeded` RPC trên Supabase.
 */
class LocalSeeder(
    private val categoryDao: CategoryDao,
    private val cardDao: CardDao,
    private val cardVariantDao: CardVariantDao,
) {

    /** Chỉ seed nếu user chưa có category nào (idempotent). */
    suspend fun seedFor(userId: String, language: String) {
        val existing = categoryDao.getAll(userId)
        if (existing.isNotEmpty()) {
            AppLog.d(AppLog.Feature.CATEGORY, "LocalSeeder", "already seeded for userId=${userId.take(8)}")
            return
        }

        val now = Instant.now().toString()
        val categories = buildDefaultCategories(userId, language, now)
        categoryDao.upsert(*categories.toTypedArray())

        val foodId = categories.firstOrNull { it.type == "expense" && it.name in setOf("Ăn uống", "Food & Drinks") }?.id
        val transportId = categories.firstOrNull { it.type == "expense" && it.name in setOf("Di chuyển", "Transport") }?.id
        val cards = buildDefaultCards(userId, language, now, foodId, transportId)
        cards.forEach { (card, variants) ->
            cardDao.upsert(card)
            if (variants.isNotEmpty()) cardVariantDao.upsert(*variants.toTypedArray())
        }

        AppLog.d(AppLog.Feature.CATEGORY, "LocalSeeder", "seeded ${categories.size} categories + ${cards.size} cards, lang=$language")
    }

    private fun buildDefaultCategories(
        userId: String,
        language: String,
        createdAt: String,
    ): List<CategoryEntity> {
        val isVi = language == "vi"
        return listOf(
            // ── Expense ──────────────────────────────────────────────────────
            cat(userId, if (isVi) "Ăn uống" else "Food & Drinks", "🍜", "#FF5722", "expense", language, createdAt),
            cat(userId, if (isVi) "Di chuyển" else "Transport", "🚌", "#2196F3", "expense", language, createdAt),
            cat(userId, if (isVi) "Mua sắm" else "Shopping", "🛍️", "#9C27B0", "expense", language, createdAt),
            cat(userId, if (isVi) "Giải trí" else "Entertainment", "🎮", "#E91E63", "expense", language, createdAt),
            cat(userId, if (isVi) "Sức khỏe" else "Health", "💊", "#4CAF50", "expense", language, createdAt),
            cat(userId, if (isVi) "Hóa đơn" else "Bills & Utilities", "⚡", "#FF9800", "expense", language, createdAt),
            cat(userId, if (isVi) "Nhà ở" else "Housing", "🏠", "#795548", "expense", language, createdAt),
            cat(userId, if (isVi) "Giáo dục" else "Education", "📚", "#00BCD4", "expense", language, createdAt),
            // ── Income ───────────────────────────────────────────────────────
            cat(userId, if (isVi) "Lương" else "Salary", "💰", "#10B981", "income", language, createdAt),
            cat(userId, if (isVi) "Thu nhập khác" else "Other Income", "🎁", "#0EA5E9", "income", language, createdAt),
        )
    }

    private fun buildDefaultCards(
        userId: String,
        language: String,
        createdAt: String,
        foodCategoryId: String?,
        transportCategoryId: String?,
    ): List<Pair<SpendingCardEntity, List<CardVariantEntity>>> {
        val isVi = language == "vi"
        return if (isVi) listOf(
            card(userId, "Ăn sáng", "expense", foodCategoryId, language, createdAt, position = 0,
                variants = listOf(25_000L to true, 35_000L to false, 50_000L to false)),
            card(userId, "Ăn trưa", "expense", foodCategoryId, language, createdAt, position = 1,
                variants = listOf(35_000L to true, 50_000L to false, 70_000L to false)),
            card(userId, "Ăn tối", "expense", foodCategoryId, language, createdAt, position = 2,
                variants = listOf(45_000L to true, 65_000L to false, 90_000L to false)),
            card(userId, "Cà phê", "expense", foodCategoryId, language, createdAt, position = 3,
                variants = listOf(25_000L to true, 45_000L to false)),
            card(userId, "Grab", "expense", transportCategoryId, language, createdAt, position = 4,
                variants = listOf(25_000L to true, 40_000L to false, 60_000L to false)),
        ) else listOf(
            card(userId, "Breakfast", "expense", foodCategoryId, language, createdAt, position = 0,
                variants = listOf(5L to true, 8L to false, 12L to false)),
            card(userId, "Lunch", "expense", foodCategoryId, language, createdAt, position = 1,
                variants = listOf(10L to true, 15L to false, 20L to false)),
            card(userId, "Dinner", "expense", foodCategoryId, language, createdAt, position = 2,
                variants = listOf(15L to true, 25L to false, 40L to false)),
            card(userId, "Coffee", "expense", foodCategoryId, language, createdAt, position = 3,
                variants = listOf(4L to true, 6L to false)),
            card(userId, "Transport", "expense", transportCategoryId, language, createdAt, position = 4,
                variants = listOf(3L to true, 8L to false, 15L to false)),
        )
    }

    private fun card(
        userId: String,
        title: String,
        type: String,
        categoryId: String?,
        language: String,
        createdAt: String,
        position: Int,
        variants: List<Pair<Long, Boolean>>,
    ): Pair<SpendingCardEntity, List<CardVariantEntity>> {
        val cardId = UUID.randomUUID().toString()
        val entity = SpendingCardEntity(
            id = cardId,
            userId = userId,
            title = title,
            categoryId = categoryId,
            type = type,
            note = null,
            position = position,
            useCount = 0,
            language = language,
            createdAt = createdAt,
            updatedAt = null,
            syncedAt = null,
            deletedAt = null,
        )
        val variantEntities = variants.mapIndexed { index, (amount, isDefault) ->
            CardVariantEntity(
                id = UUID.randomUUID().toString(),
                cardId = cardId,
                label = null,
                amount = amount,
                isDefault = isDefault,
                position = index,
                createdAt = createdAt,
                syncedAt = null,
            )
        }
        return entity to variantEntities
    }

    private fun cat(
        userId: String,
        name: String,
        icon: String,
        color: String,
        type: String,
        language: String,
        createdAt: String,
    ): CategoryEntity = CategoryEntity(
        id = UUID.randomUUID().toString(),
        userId = userId,
        name = name,
        icon = icon,
        color = color,
        type = type,
        language = language,
        createdAt = createdAt,
        updatedAt = createdAt,
        syncedAt = null,
        deletedAt = null,
    )
}
