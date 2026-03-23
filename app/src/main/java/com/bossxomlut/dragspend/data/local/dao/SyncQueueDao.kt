package com.bossxomlut.dragspend.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bossxomlut.dragspend.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    @Query("SELECT * FROM sync_queue ORDER BY created_at ASC")
    suspend fun getAll(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue ORDER BY created_at ASC")
    fun getAllFlow(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE table_name = :tableName ORDER BY created_at ASC")
    suspend fun getByTable(tableName: String): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE record_id = :recordId AND table_name = :tableName ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestForRecord(recordId: String, tableName: String): SyncQueueEntity?

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun getCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SyncQueueEntity): Long

    @Update
    suspend fun update(entity: SyncQueueEntity)

    @Delete
    suspend fun delete(entity: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_queue WHERE record_id = :recordId AND table_name = :tableName")
    suspend fun deleteByRecord(recordId: String, tableName: String)

    @Query("DELETE FROM sync_queue")
    suspend fun deleteAll()

    @Query("UPDATE sync_queue SET retry_count = retry_count + 1, last_error = :error WHERE id = :id")
    suspend fun incrementRetryCount(id: Long, error: String?)
}
