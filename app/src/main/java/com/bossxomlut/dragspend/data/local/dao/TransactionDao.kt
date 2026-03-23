package com.bossxomlut.dragspend.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bossxomlut.dragspend.data.local.entity.SyncStatus
import com.bossxomlut.dragspend.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // ─── Queries ───

    @Query("SELECT * FROM transactions WHERE user_id = :userId AND date = :date AND sync_status != :excludeStatus ORDER BY position ASC")
    fun getByDateFlow(
        userId: String,
        date: String,
        excludeStatus: SyncStatus = SyncStatus.PENDING_DELETE,
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE user_id = :userId AND date = :date ORDER BY position ASC")
    suspend fun getByDate(userId: String, date: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE user_id = :userId AND date >= :startDate AND date < :endDate ORDER BY date ASC, position ASC")
    suspend fun getByDateRange(userId: String, startDate: String, endDate: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE user_id = :userId AND date LIKE :yearMonth || '%' ORDER BY date ASC, position ASC")
    suspend fun getByMonth(userId: String, yearMonth: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE user_id = :userId AND date >= :startDate AND date < :endDate AND sync_status != :excludeStatus ORDER BY date ASC, position ASC")
    fun getByDateRangeFlow(
        userId: String,
        startDate: String,
        endDate: String,
        excludeStatus: SyncStatus = SyncStatus.PENDING_DELETE,
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT MAX(position) FROM transactions WHERE user_id = :userId AND date = :date")
    suspend fun getMaxPosition(userId: String, date: String): Int?

    // ─── Sync Queries ───

    @Query("SELECT * FROM transactions WHERE sync_status != :syncedStatus")
    suspend fun getPendingSync(syncedStatus: SyncStatus = SyncStatus.SYNCED): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE sync_status = :status")
    suspend fun getByStatus(status: SyncStatus): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE sync_status != :syncedStatus")
    fun getPendingSyncCountFlow(syncedStatus: SyncStatus = SyncStatus.SYNCED): Flow<Int>

    // ─── Insert/Update/Delete ───

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TransactionEntity>)

    @Update
    suspend fun update(entity: TransactionEntity)

    @Delete
    suspend fun delete(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("UPDATE transactions SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("UPDATE transactions SET sync_status = :status, server_updated_at = :serverUpdatedAt WHERE id = :id")
    suspend fun markSynced(id: String, status: SyncStatus = SyncStatus.SYNCED, serverUpdatedAt: String?)
}
