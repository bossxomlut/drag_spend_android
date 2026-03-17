package com.bossxomlut.dragspend.data.repository

import com.bossxomlut.dragspend.data.model.CardVariant
import com.bossxomlut.dragspend.data.model.SpendingCard
import com.bossxomlut.dragspend.domain.repository.CardRepository
import com.bossxomlut.dragspend.domain.repository.CreateCardRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CardRepositoryImpl(
    private val supabase: SupabaseClient,
) : CardRepository {

    override suspend fun getCards(userId: String): Result<List<SpendingCard>> = runCatching {
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
        cards.map { card -> card.copy(variants = variantsByCard[card.id] ?: emptyList()) }
    }

    override suspend fun createCard(request: CreateCardRequest): Result<SpendingCard> = runCatching {
        val cardRow = mapOf(
            "user_id" to request.userId,
            "title" to request.title,
            "category_id" to request.categoryId,
            "type" to request.type.name.lowercase(),
            "note" to request.note,
            "language" to request.language,
        )
        val card = supabase.from("spending_cards")
            .insert(cardRow) { select() }
            .decodeSingle<SpendingCard>()

        if (request.variants.isNotEmpty()) {
            val variantRows = request.variants.mapIndexed { index, v ->
                mapOf(
                    "card_id" to card.id,
                    "label" to v.label,
                    "amount" to v.amount,
                    "is_default" to v.isDefault,
                    "position" to index,
                )
            }
            val savedVariants = supabase.from("card_variants")
                .insert(variantRows) { select() }
                .decodeList<CardVariant>()
            card.copy(variants = savedVariants)
        } else {
            card
        }
    }

    override suspend fun updateCard(cardId: String, request: CreateCardRequest): Result<SpendingCard> = runCatching {
        val cardRow = mapOf(
            "title" to request.title,
            "category_id" to request.categoryId,
            "type" to request.type.name.lowercase(),
            "note" to request.note,
        )
        val card = supabase.from("spending_cards")
            .update(cardRow) {
                filter { eq("id", cardId) }
                select()
            }
            .decodeSingle<SpendingCard>()

        supabase.from("card_variants")
            .delete { filter { eq("card_id", cardId) } }

        val variantRows = request.variants.mapIndexed { index, v ->
            mapOf(
                "card_id" to card.id,
                "label" to v.label,
                "amount" to v.amount,
                "is_default" to v.isDefault,
                "position" to index,
            )
        }
        val savedVariants = if (variantRows.isNotEmpty()) {
            supabase.from("card_variants")
                .insert(variantRows) { select() }
                .decodeList<CardVariant>()
        } else {
            emptyList()
        }
        card.copy(variants = savedVariants)
    }

    override suspend fun deleteCard(cardId: String): Result<Unit> = runCatching {
        supabase.from("spending_cards")
            .delete { filter { eq("id", cardId) } }
        Unit
    }

    override suspend fun incrementUseCount(cardId: String): Result<Unit> = runCatching {
        val params = buildJsonObject { put("card_id", cardId) }
        supabase.postgrest.rpc("increment_card_use_count", params)
        Unit
    }
}
