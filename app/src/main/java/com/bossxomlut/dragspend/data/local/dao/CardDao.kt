package com.bossxomlut.dragspend.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bossxomlut.dragspend.data.local.entity.CardVariantEntity
import com.bossxomlut.dragspend.data.local.entity.SpendingCardEntity

@Dao
interface CardDao {

    @Query(
        """
        SELECT * FROM spending_cards
        WHERE user_id = :userId AND deleted_at IS NULL
        ORDER BY use_count DESC, position ASC
        """,
    )
    suspend fun getAll(userId: String): List<SpendingCardEntity>

    @Query("SELECT * FROM spending_cards WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): SpendingCardEntity?

    @Upsert
    suspend fun upsert(vararg entities: SpendingCardEntity)

    @Query("DELETE FROM spending_cards WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE spending_cards SET use_count = use_count + 1 WHERE id = :id")
    suspend fun incrementUseCount(id: String)

    @Query("DELETE FROM spending_cards WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface CardVariantDao {

    @Query(
        """
        SELECT * FROM card_variants WHERE card_id = :cardId ORDER BY position ASC
        """,
    )
    suspend fun getByCardId(cardId: String): List<CardVariantEntity>

    @Query(
        """
        SELECT * FROM card_variants WHERE card_id IN (:cardIds) ORDER BY position ASC
        """,
    )
    suspend fun getByCardIds(cardIds: List<String>): List<CardVariantEntity>

    @Upsert
    suspend fun upsert(vararg entities: CardVariantEntity)

    @Query("DELETE FROM card_variants WHERE card_id = :cardId")
    suspend fun deleteByCardId(cardId: String)

    @Query("DELETE FROM card_variants WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        """
        DELETE FROM card_variants
        WHERE card_id IN (SELECT id FROM spending_cards WHERE user_id = :userId)
        """,
    )
    suspend fun deleteAllForUser(userId: String)
}
