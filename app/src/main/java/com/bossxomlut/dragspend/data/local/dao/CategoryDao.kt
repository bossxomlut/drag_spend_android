package com.bossxomlut.dragspend.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bossxomlut.dragspend.data.local.entity.CategoryEntity
import com.bossxomlut.dragspend.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // ─── Queries ───

    @Query("SELECT * FROM categories WHERE user_id = :userId AND sync_status != :excludeStatus ORDER BY name ASC")
    fun getAllFlow(
        userId: String,
        excludeStatus: SyncStatus = SyncStatus.PENDING_DELETE,
    ): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE user_id = :userId ORDER BY name ASC")
    suspend fun getAll(userId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<CategoryEntity>

    // ─── Sync Queries ───

    @Query("SELECT * FROM categories WHERE sync_status != :syncedStatus")
    suspend fun getPendingSync(syncedStatus: SyncStatus = SyncStatus.SYNCED): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories WHERE sync_status != :syncedStatus")
    fun getPendingSyncCountFlow(syncedStatus: SyncStatus = SyncStatus.SYNCED): Flow<Int>

    // ─── Insert/Update/Delete ───

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CategoryEntity>)

    @Update
    suspend fun update(entity: CategoryEntity)

    @Delete
    suspend fun delete(entity: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM categories WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("UPDATE categories SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("UPDATE categories SET sync_status = :status, server_updated_at = :serverUpdatedAt WHERE id = :id")
    suspend fun markSynced(id: String, status: SyncStatus = SyncStatus.SYNCED, serverUpdatedAt: String?)
}
