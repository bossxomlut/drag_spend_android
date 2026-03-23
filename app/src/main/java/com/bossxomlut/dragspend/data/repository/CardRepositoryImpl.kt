package com.bossxomlut.dragspend.data.repository

import com.bossxomlut.dragspend.data.local.dao.CardVariantDao
import com.bossxomlut.dragspend.data.local.dao.SpendingCardDao
import com.bossxomlut.dragspend.data.local.entity.CardVariantEntity
import com.bossxomlut.dragspend.data.local.entity.SpendingCardEntity
import com.bossxomlut.dragspend.data.local.entity.SyncStatus
import com.bossxomlut.dragspend.data.model.CardVariant
import com.bossxomlut.dragspend.data.model.Category
import com.bossxomlut.dragspend.data.model.SpendingCard
import com.bossxomlut.dragspend.data.sync.ConnectivityMonitor
import com.bossxomlut.dragspend.data.sync.SyncManager
import com.bossxomlut.dragspend.domain.repository.CardRepository
import com.bossxomlut.dragspend.domain.repository.CategoryRepository
import com.bossxomlut.dragspend.domain.repository.CreateCardRequest
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.logResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

class CardRepositoryImpl(
    private val supabase: SupabaseClient,
    private val categoryRepository: CategoryRepository,
    private val spendingCardDao: SpendingCardDao,
    private val cardVariantDao: CardVariantDao,
    private val connectivityMonitor: ConnectivityMonitor,
    private val syncManager: SyncManager,
) : CardRepository {

    /**
     * Returns a Flow of spending cards for the given user.
     * This is the reactive, local-first API for UI consumption.
     */
    fun getCardsFlow(userId: String): Flow<List<SpendingCard>> {
        return spendingCardDao.getAllFlow(userId).map { cardEntities ->
            if (cardEntities.isEmpty()) return@map emptyList()

            val cardIds = cardEntities.map { it.id }
            val variants = cardVariantDao.getByCardIds(cardIds)
            val variantsByCard = variants.groupBy { it.cardId }

            // Attach categories
            val categoryIds = cardEntities.mapNotNull { it.categoryId }.distinct()
            val categories = if (categoryIds.isNotEmpty()) {
                categoryRepository.getCategories(userId).getOrNull() ?: emptyList()
            } else {
                emptyList()
            }
            val categoryById = categories.associateBy { it.id }

            cardEntities.map { entity ->
                entity.toDomain(
                    variants = variantsByCard[entity.id]?.map { it.toDomain() } ?: emptyList(),
                    category = entity.categoryId?.let { categoryById[it] },
                )
            }
        }
    }

    override suspend fun getCards(userId: String): Result<List<SpendingCard>> = runCatching {
        AppLog.d(AppLog.Feature.CARD, "getCards", "userId=${userId.take(8)}")

        // Try local first
        val localCards = spendingCardDao.getAll(userId)
            .filter { it.syncStatus != SyncStatus.PENDING_DELETE }

        if (localCards.isNotEmpty()) {
            val assembledCards = assembleCardsFromLocal(userId, localCards)
            
            // Check if any cards have variants - if none do and we have remote access, refresh
            val hasAnyVariants = assembledCards.any { it.variants.isNotEmpty() }
            val allCardsHaveNoVariants = assembledCards.none { it.variants.isNotEmpty() }
            
            // If we have cards but NO variants at all, and we're online, refresh from server
            // This handles the case where variants weren't cached properly
            if (allCardsHaveNoVariants && connectivityMonitor.isConnected()) {
                AppLog.d(AppLog.Feature.CARD, "getCards", "Local cards have no variants, refreshing from server")
            } else {
                AppLog.d(AppLog.Feature.CARD, "getCards", "Found ${localCards.size} local cards with variants")
                return@runCatching assembledCards
            }
        }

        // Fallback to remote if online
        if (!connectivityMonitor.isConnected()) {
            AppLog.d(AppLog.Feature.CARD, "getCards", "Offline, returning local cards or empty")
            return@runCatching if (localCards.isNotEmpty()) {
                assembleCardsFromLocal(userId, localCards)
            } else {
                emptyList()
            }
        }

        // Fetch from server
        val cards = supabase.from("spending_cards")
            .select {
                filter { eq("user_id", userId) }
                order("use_count", order = Order.DESCENDING)
                order("position", order = Order.ASCENDING)
            }
            .decodeList<SpendingCard>()

        val cardIds = cards.map { it.id }
        val variants = if (cardIds.isNotEmpty()) {
            supabase.from("card_variants")
                .select {
                    filter { isIn("card_id", cardIds) }
                    order("position", order = Order.ASCENDING)
                }
                .decodeList<CardVariant>()
        } else {
            emptyList()
        }

        val variantsByCard = variants.groupBy { it.cardId }

        val categoryIds = cards.mapNotNull { it.categoryId }.distinct()
        val categories = if (categoryIds.isNotEmpty()) {
            supabase.from("categories")
                .select { filter { isIn("id", categoryIds) } }
                .decodeList<Category>()
        } else {
            emptyList()
        }
        val categoryById = categories.associateBy { it.id }

        val assembledCards = cards.map { card ->
            card.copy(
                variants = variantsByCard[card.id] ?: emptyList(),
                category = card.categoryId?.let { categoryById[it] },
            )
        }

        // Cache to local
        cacheCardsToLocal(assembledCards)

        assembledCards
    }.logResult(AppLog.Feature.CARD, "getCards") { "${it.size} cards" }

    override suspend fun createCard(request: CreateCardRequest): Result<SpendingCard> = runCatching {
        AppLog.d(AppLog.Feature.CARD, "createCard", "title=${request.title}, type=${request.type}")

        val newId = UUID.randomUUID().toString()
        val now = java.time.Instant.now().toString()

        // Create card domain object
        val card = SpendingCard(
            id = newId,
            userId = request.userId,
            title = request.title,
            categoryId = request.categoryId,
            type = request.type,
            note = request.note,
            position = 0,
            useCount = 0,
            language = request.language,
            createdAt = now,
            updatedAt = now,
            category = null,
            variants = emptyList(),
        )

        // Create variant domain objects
        val variants = request.variants.mapIndexed { index, v ->
            CardVariant(
                id = UUID.randomUUID().toString(),
                cardId = newId,
                label = v.label,
                amount = v.amount,
                isDefault = v.isDefault,
                position = index,
                createdAt = now,
            )
        }

        if (connectivityMonitor.isConnected()) {
            // Try to sync immediately
            try {
                val serverCard = supabase.from("spending_cards")
                    .insert(request.toJsonObject()) { select() }
                    .decodeSingle<SpendingCard>()

                val savedVariants = if (request.variants.isNotEmpty()) {
                    val variantRows = request.variants.mapIndexed { index, v -> v.toJsonObject(serverCard.id, index) }
                    supabase.from("card_variants")
                        .insert(variantRows) { select() }
                        .decodeList<CardVariant>()
                } else {
                    emptyList()
                }

                val resultCard = serverCard.copy(variants = savedVariants)

                // Cache to local
                spendingCardDao.insert(SpendingCardEntity.fromDomain(resultCard, SyncStatus.SYNCED, resultCard.updatedAt))
                savedVariants.forEach { v ->
                    cardVariantDao.insert(CardVariantEntity.fromDomain(v, SyncStatus.SYNCED))
                }

                return@runCatching resultCard
            } catch (e: Exception) {
                AppLog.e(AppLog.Feature.CARD, "createCard", "Remote failed: ${e.message}")
            }
        }

        // Save locally with PENDING_CREATE status
        spendingCardDao.insert(SpendingCardEntity.fromDomain(card, SyncStatus.PENDING_CREATE, null))
        variants.forEach { v ->
            cardVariantDao.insert(CardVariantEntity.fromDomain(v, SyncStatus.PENDING_CREATE))
        }

        syncManager.triggerSync()
        card.copy(variants = variants)
    }.logResult(AppLog.Feature.CARD, "createCard") { "id=${it.id}" }

    override suspend fun updateCard(cardId: String, request: CreateCardRequest): Result<SpendingCard> = runCatching {
        AppLog.d(AppLog.Feature.CARD, "updateCard", "id=$cardId, title=${request.title}")

        val existing = spendingCardDao.getById(cardId)
            ?: throw IllegalStateException("Card not found: $cardId")

        val now = java.time.Instant.now().toString()
        val newSyncStatus = when (existing.syncStatus) {
            SyncStatus.PENDING_CREATE -> SyncStatus.PENDING_CREATE
            else -> SyncStatus.PENDING_UPDATE
        }

        // Update local card
        val updatedEntity = existing.copy(
            title = request.title,
            categoryId = request.categoryId,
            type = request.type.name.lowercase(),
            note = request.note,
            language = request.language,
            updatedAt = now,
            syncStatus = newSyncStatus,
        )
        spendingCardDao.update(updatedEntity)

        // Delete old variants and insert new ones
        cardVariantDao.deleteByCardId(cardId)
        val newVariants = request.variants.mapIndexed { index, v ->
            CardVariant(
                id = UUID.randomUUID().toString(),
                cardId = cardId,
                label = v.label,
                amount = v.amount,
                isDefault = v.isDefault,
                position = index,
                createdAt = now,
            )
        }
        newVariants.forEach { v ->
            cardVariantDao.insert(CardVariantEntity.fromDomain(v, newSyncStatus))
        }

        if (connectivityMonitor.isConnected()) {
            try {
                val serverCard = supabase.from("spending_cards")
                    .update(request.toJsonObject()) {
                        filter { eq("id", cardId) }
                        select()
                    }
                    .decodeSingle<SpendingCard>()

                supabase.from("card_variants")
                    .delete { filter { eq("card_id", cardId) } }

                val savedVariants = if (request.variants.isNotEmpty()) {
                    val variantRows = request.variants.mapIndexed { index, v -> v.toJsonObject(cardId, index) }
                    supabase.from("card_variants")
                        .insert(variantRows) { select() }
                        .decodeList<CardVariant>()
                } else {
                    emptyList()
                }

                // Update local with synced data
                spendingCardDao.markSynced(cardId, SyncStatus.SYNCED, serverCard.updatedAt)
                cardVariantDao.deleteByCardId(cardId)
                savedVariants.forEach { v ->
                    cardVariantDao.insert(CardVariantEntity.fromDomain(v, SyncStatus.SYNCED))
                }

                return@runCatching serverCard.copy(variants = savedVariants)
            } catch (e: Exception) {
                AppLog.e(AppLog.Feature.CARD, "updateCard", "Remote failed: ${e.message}")
                syncManager.triggerSync()
            }
        } else {
            syncManager.triggerSync()
        }

        updatedEntity.toDomain(variants = newVariants)
    }.logResult(AppLog.Feature.CARD, "updateCard") { "id=${it.id}" }

    override suspend fun deleteCard(cardId: String): Result<Unit> = runCatching {
        AppLog.d(AppLog.Feature.CARD, "deleteCard", "id=$cardId")

        val existing = spendingCardDao.getById(cardId)

        if (existing?.syncStatus == SyncStatus.PENDING_CREATE) {
            // Never synced, just delete locally
            spendingCardDao.deleteById(cardId)
            cardVariantDao.deleteByCardId(cardId)
        } else {
            // Mark for deletion
            existing?.let {
                spendingCardDao.updateSyncStatus(cardId, SyncStatus.PENDING_DELETE)
            }

            if (connectivityMonitor.isConnected()) {
                try {
                    supabase.from("spending_cards")
                        .delete { filter { eq("id", cardId) } }
                    spendingCardDao.deleteById(cardId)
                    cardVariantDao.deleteByCardId(cardId)
                } catch (e: Exception) {
                    AppLog.e(AppLog.Feature.CARD, "deleteCard", "Remote failed: ${e.message}")
                    syncManager.triggerSync()
                }
            } else {
                syncManager.triggerSync()
            }
        }
        Unit
    }.logResult(AppLog.Feature.CARD, "deleteCard") { "deleted" }

    override suspend fun incrementUseCount(cardId: String): Result<Unit> = runCatching {
        AppLog.d(AppLog.Feature.CARD, "incrementUseCount", "id=$cardId")

        // Always update local
        spendingCardDao.incrementUseCount(cardId)

        // Try to sync to server if online
        if (connectivityMonitor.isConnected()) {
            try {
                val params = buildJsonObject { put("card_id", cardId) }
                supabase.postgrest.rpc("increment_card_use_count", params)
            } catch (e: Exception) {
                AppLog.e(AppLog.Feature.CARD, "incrementUseCount", "Remote failed: ${e.message}")
                // Use count is not critical, no need to queue for sync
            }
        }
        Unit
    }.logResult(AppLog.Feature.CARD, "incrementUseCount") { "ok" }

    /**
     * Assembles SpendingCard domain objects from local entities.
     */
    private suspend fun assembleCardsFromLocal(
        userId: String,
        cardEntities: List<SpendingCardEntity>,
    ): List<SpendingCard> {
        if (cardEntities.isEmpty()) return emptyList()

        val cardIds = cardEntities.map { it.id }
        val variants = cardVariantDao.getByCardIds(cardIds)
        AppLog.d(AppLog.Feature.CARD, "assembleCardsFromLocal", "Found ${variants.size} variants for ${cardIds.size} cards")
        val variantsByCard = variants.groupBy { it.cardId }

        // Get categories
        val categoryIds = cardEntities.mapNotNull { it.categoryId }.distinct()
        val categories = if (categoryIds.isNotEmpty()) {
            categoryRepository.getCategories(userId).getOrNull() ?: emptyList()
        } else {
            emptyList()
        }
        val categoryById = categories.associateBy { it.id }

        return cardEntities.map { entity ->
            val cardVariants = variantsByCard[entity.id]?.map { it.toDomain() } ?: emptyList()
            AppLog.d(AppLog.Feature.CARD, "assembleCardsFromLocal", "Card ${entity.id.take(8)} has ${cardVariants.size} variants")
            entity.toDomain(
                variants = cardVariants,
                category = entity.categoryId?.let { categoryById[it] },
            )
        }
    }

    /**
     * Caches cards and their variants to local database.
     * Clears existing variants for the cards before inserting to avoid stale data.
     */
    private suspend fun cacheCardsToLocal(cards: List<SpendingCard>) {
        AppLog.d(AppLog.Feature.CARD, "cacheCardsToLocal", "Caching ${cards.size} cards")
        cards.forEach { card ->
            // Insert or update the card
            spendingCardDao.insert(SpendingCardEntity.fromDomain(card, SyncStatus.SYNCED, card.updatedAt))
            
            // Clear existing variants for this card to avoid duplicates
            cardVariantDao.deleteByCardId(card.id)
            
            // Insert all variants
            AppLog.d(AppLog.Feature.CARD, "cacheCardsToLocal", "Card ${card.id.take(8)} has ${card.variants.size} variants")
            card.variants.forEach { variant ->
                cardVariantDao.insert(CardVariantEntity.fromDomain(variant, SyncStatus.SYNCED))
            }
        }
    }
}
