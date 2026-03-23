package com.bossxomlut.dragspend.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.bossxomlut.dragspend.data.local.entity.CardVariantEntity
import com.bossxomlut.dragspend.data.local.entity.SpendingCardEntity
import com.bossxomlut.dragspend.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SpendingCardDao {

    // ─── Queries ───

    @Query("SELECT * FROM spending_cards WHERE user_id = :userId AND sync_status != :excludeStatus ORDER BY use_count DESC, position ASC")
    fun getAllFlow(
        userId: String,
        excludeStatus: SyncStatus = SyncStatus.PENDING_DELETE,
    ): Flow<List<SpendingCardEntity>>

    @Query("SELECT * FROM spending_cards WHERE user_id = :userId ORDER BY use_count DESC, position ASC")
    suspend fun getAll(userId: String): List<SpendingCardEntity>

    @Query("SELECT * FROM spending_cards WHERE id = :id")
    suspend fun getById(id: String): SpendingCardEntity?

    // ─── Sync Queries ───

    @Query("SELECT * FROM spending_cards WHERE sync_status != :syncedStatus")
    suspend fun getPendingSync(syncedStatus: SyncStatus = SyncStatus.SYNCED): List<SpendingCardEntity>

    @Query("SELECT COUNT(*) FROM spending_cards WHERE sync_status != :syncedStatus")
    fun getPendingSyncCountFlow(syncedStatus: SyncStatus = SyncStatus.SYNCED): Flow<Int>

    // ─── Insert/Update/Delete ───

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SpendingCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SpendingCardEntity>)

    @Update
    suspend fun update(entity: SpendingCardEntity)

    @Delete
    suspend fun delete(entity: SpendingCardEntity)

    @Query("DELETE FROM spending_cards WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM spending_cards WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("UPDATE spending_cards SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("UPDATE spending_cards SET sync_status = :status, server_updated_at = :serverUpdatedAt WHERE id = :id")
    suspend fun markSynced(id: String, status: SyncStatus = SyncStatus.SYNCED, serverUpdatedAt: String?)

    @Query("UPDATE spending_cards SET use_count = use_count + 1 WHERE id = :id")
    suspend fun incrementUseCount(id: String)
}

@Dao
interface CardVariantDao {

    @Query("SELECT * FROM card_variants WHERE card_id = :cardId ORDER BY position ASC")
    suspend fun getByCardId(cardId: String): List<CardVariantEntity>

    @Query("SELECT * FROM card_variants WHERE card_id IN (:cardIds) ORDER BY position ASC")
    suspend fun getByCardIds(cardIds: List<String>): List<CardVariantEntity>

    @Query("SELECT * FROM card_variants WHERE card_id IN (:cardIds) ORDER BY position ASC")
    fun getByCardIdsFlow(cardIds: List<String>): Flow<List<CardVariantEntity>>

    @Query("SELECT * FROM card_variants WHERE id = :id")
    suspend fun getById(id: String): CardVariantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CardVariantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CardVariantEntity>)

    @Update
    suspend fun update(entity: CardVariantEntity)

    @Delete
    suspend fun delete(entity: CardVariantEntity)

    @Query("DELETE FROM card_variants WHERE card_id = :cardId")
    suspend fun deleteByCardId(cardId: String)

    @Query("DELETE FROM card_variants WHERE id = :id")
    suspend fun deleteById(id: String)

    // ─── Sync Queries ───

    @Query("SELECT * FROM card_variants WHERE sync_status != :syncedStatus")
    suspend fun getPendingSync(syncedStatus: SyncStatus = SyncStatus.SYNCED): List<CardVariantEntity>

    @Query("UPDATE card_variants SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)
}
