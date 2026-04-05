package com.bossxomlut.dragspend.data.repository

import com.bossxomlut.dragspend.data.local.dao.CardDao
import com.bossxomlut.dragspend.data.local.dao.CardVariantDao
import com.bossxomlut.dragspend.data.local.dao.CategoryDao
import com.bossxomlut.dragspend.data.local.entity.CardVariantEntity
import com.bossxomlut.dragspend.data.local.entity.SpendingCardEntity
import com.bossxomlut.dragspend.data.local.toDomain
import com.bossxomlut.dragspend.data.local.toEntity
import com.bossxomlut.dragspend.data.model.CardVariantDto
import com.bossxomlut.dragspend.data.model.SpendingCardDto
import com.bossxomlut.dragspend.data.model.toDomain
import com.bossxomlut.dragspend.domain.error.mapToAppError
import com.bossxomlut.dragspend.domain.model.SpendingCard
import com.bossxomlut.dragspend.domain.repository.CardRepository
import com.bossxomlut.dragspend.domain.repository.CreateCardRequest
import com.bossxomlut.dragspend.domain.repository.SessionRepository
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.logResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CardRepositoryImpl(
    private val supabase: SupabaseClient,
    private val cardDao: CardDao,
    private val cardVariantDao: CardVariantDao,
    private val categoryDao: CategoryDao,
    private val sessionRepository: SessionRepository,
) : CardRepository {

    override suspend fun getCards(userId: String): Result<List<SpendingCard>> = runCatching {
        AppLog.d(AppLog.Feature.CARD, "getCards", "userId=${userId.take(8)}")
        val localCards = cardDao.getAll(userId)
        if (localCards.isEmpty()) return@runCatching emptyList()
        val cardIds = localCards.map { it.id }
        val variantsByCard = cardVariantDao.getByCardIds(cardIds).groupBy { it.cardId }
        val categoryById = categoryDao.getAll(userId).associateBy({ it.id }, { it.toDomain() })
        localCards.map { card ->
            val variants = (variantsByCard[card.id] ?: emptyList()).map { it.toDomain() }
            card.toDomain(
                category = card.categoryId?.let { categoryById[it] },
                variants = variants,
            )
        }
    }.logResult(AppLog.Feature.CARD, "getCards") { "${it.size} cards" }
        .mapToAppError()

    override suspend fun createCard(request: CreateCardRequest): Result<SpendingCard> = runCatching {
        AppLog.d(AppLog.Feature.CARD, "createCard", "title=${request.title}, type=${request.type}")
        val localCardId = UUID.randomUUID().toString()
        val now = Instant.now().toString()

        if (!sessionRepository.isAuthenticated()) {
            val cardEntity = SpendingCardEntity(
                id = localCardId,
                userId = request.userId,
                title = request.title,
                categoryId = request.categoryId,
                type = request.type.name.lowercase(),
                note = request.note,
                position = 0,
                useCount = 0,
                language = request.language,
                createdAt = now,
                updatedAt = null,
                syncedAt = null,
                deletedAt = null,
            )
            cardDao.upsert(cardEntity)
            val variantEntities = request.variants.mapIndexed { index, v ->
                CardVariantEntity(
                    id = UUID.randomUUID().toString(),
                    cardId = localCardId,
                    label = v.label,
                    amount = v.amount,
                    isDefault = v.isDefault,
                    position = index,
                    createdAt = now,
                    syncedAt = null,
                )
            }
            if (variantEntities.isNotEmpty()) cardVariantDao.upsert(*variantEntities.toTypedArray())
            return@runCatching cardEntity.toDomain(category = null, variants = variantEntities.map { it.toDomain() })
        }

        val insertJson = buildJsonObject {
            put("id", localCardId)
            request.toJsonObject().entries.forEach { (k, v) -> put(k, v) }
        }
        val cardDto = supabase.from("spending_cards")
            .insert(insertJson) { select() }
            .decodeSingle<SpendingCardDto>()
        val savedVariants = if (request.variants.isNotEmpty()) {
            val variantRows = request.variants.mapIndexed { index, v -> v.toJsonObject(cardDto.id, index) }
            supabase.from("card_variants")
                .insert(variantRows) { select() }
                .decodeList<CardVariantDto>()
        } else {
            emptyList()
        }
        cardDao.upsert(cardDto.toEntity(syncedAt = now))
        if (savedVariants.isNotEmpty()) cardVariantDao.upsert(*savedVariants.map { it.toEntity(syncedAt = now) }.toTypedArray())
        cardDto.toDomain(category = null, variants = savedVariants.map { it.toDomain() })
    }.logResult(AppLog.Feature.CARD, "createCard") { "id=${it.id}" }
        .mapToAppError()

    override suspend fun updateCard(cardId: String, request: CreateCardRequest): Result<SpendingCard> = runCatching {
        AppLog.d(AppLog.Feature.CARD, "updateCard", "id=$cardId, title=${request.title}")
        val now = Instant.now().toString()

        if (!sessionRepository.isAuthenticated()) {
            val existing = cardDao.getById(cardId)
            val updated = existing?.copy(
                title = request.title,
                categoryId = request.categoryId,
                type = request.type.name.lowercase(),
                note = request.note,
                language = request.language,
                updatedAt = now,
            ) ?: return@runCatching error("Card $cardId not found locally")
            cardDao.upsert(updated)
            cardVariantDao.deleteByCardId(cardId)
            val newVariants = request.variants.mapIndexed { index, v ->
                CardVariantEntity(
                    id = UUID.randomUUID().toString(),
                    cardId = cardId,
                    label = v.label,
                    amount = v.amount,
                    isDefault = v.isDefault,
                    position = index,
                    createdAt = now,
                    syncedAt = null,
                )
            }
            if (newVariants.isNotEmpty()) cardVariantDao.upsert(*newVariants.toTypedArray())
            return@runCatching updated.toDomain(category = null, variants = newVariants.map { it.toDomain() })
        }

        val cardDto = supabase.from("spending_cards")
            .update(request.toJsonObject()) {
                filter { eq("id", cardId) }
                select()
            }
            .decodeSingle<SpendingCardDto>()
        supabase.from("card_variants").delete { filter { eq("card_id", cardId) } }
        val savedVariants = if (request.variants.isNotEmpty()) {
            val variantRows = request.variants.mapIndexed { index, v -> v.toJsonObject(cardDto.id, index) }
            supabase.from("card_variants")
                .insert(variantRows) { select() }
                .decodeList<CardVariantDto>()
        } else {
            emptyList()
        }
        cardDao.upsert(cardDto.toEntity(syncedAt = now))
        cardVariantDao.deleteByCardId(cardId)
        if (savedVariants.isNotEmpty()) cardVariantDao.upsert(*savedVariants.map { it.toEntity(syncedAt = now) }.toTypedArray())
        cardDto.toDomain(category = null, variants = savedVariants.map { it.toDomain() })
    }.logResult(AppLog.Feature.CARD, "updateCard") { "id=${it.id}" }
        .mapToAppError()

    override suspend fun deleteCard(cardId: String): Result<Unit> = runCatching {
        AppLog.d(AppLog.Feature.CARD, "deleteCard", "id=$cardId")
        cardVariantDao.deleteByCardId(cardId)
        cardDao.deleteById(cardId)
        if (sessionRepository.isAuthenticated()) {
            supabase.from("spending_cards").delete { filter { eq("id", cardId) } }
        }
        Unit
    }.logResult(AppLog.Feature.CARD, "deleteCard") { "deleted" }
        .mapToAppError()

    override suspend fun incrementUseCount(cardId: String): Result<Unit> = runCatching {
        AppLog.d(AppLog.Feature.CARD, "incrementUseCount", "id=$cardId")
        cardDao.incrementUseCount(cardId)
        if (sessionRepository.isAuthenticated()) {
            val params = buildJsonObject { put("card_id", cardId) }
            supabase.postgrest.rpc("increment_card_use_count", params)
        }
        Unit
    }.logResult(AppLog.Feature.CARD, "incrementUseCount") { "ok" }
        .mapToAppError()
}
