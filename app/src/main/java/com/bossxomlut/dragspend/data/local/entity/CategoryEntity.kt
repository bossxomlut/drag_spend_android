package com.bossxomlut.dragspend.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index("user_id")],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val name: String,
    val icon: String,
    val color: String,
    val type: String,
    val language: String,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "synced_at") val syncedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)
