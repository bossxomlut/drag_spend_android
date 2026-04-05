package com.bossxomlut.dragspend.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("user_id"),
        Index(value = ["user_id", "date"]),
        Index("category_id"),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "source_card_id") val sourceCardId: String?,
    val date: String,
    val title: String,
    val amount: Long,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    val type: String,
    val note: String?,
    val position: Int,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "synced_at") val syncedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)
