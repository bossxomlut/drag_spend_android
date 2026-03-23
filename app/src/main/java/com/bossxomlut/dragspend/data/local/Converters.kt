package com.bossxomlut.dragspend.data.local

import androidx.room.TypeConverter
import com.bossxomlut.dragspend.data.local.entity.SyncStatus

/**
 * Room type converters for enum classes.
 */
class Converters {

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}
