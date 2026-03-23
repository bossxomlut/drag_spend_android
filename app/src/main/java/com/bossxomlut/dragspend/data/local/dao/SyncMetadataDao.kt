package com.bossxomlut.dragspend.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bossxomlut.dragspend.data.local.entity.SyncMetadataEntity

@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE table_name = :tableName")
    suspend fun get(tableName: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata")
    suspend fun getAll(): List<SyncMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata WHERE table_name = :tableName")
    suspend fun delete(tableName: String)

    @Query("DELETE FROM sync_metadata")
    suspend fun deleteAll()
}
