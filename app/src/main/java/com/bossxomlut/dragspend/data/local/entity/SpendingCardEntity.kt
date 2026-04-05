package com.bossxomlut.dragspend.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "spending_cards",
    indices = [Index("user_id")],
)
data class SpendingCardEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val title: String,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    val type: String,
    val note: String?,
    val position: Int,
    @ColumnInfo(name = "use_count") val useCount: Int,
    val language: String,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "synced_at") val syncedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(
    tableName = "card_variants",
    indices = [Index("card_id")],
)
data class CardVariantEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "card_id") val cardId: String,
    val label: String?,
    val amount: Long,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
    val position: Int,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "synced_at") val syncedAt: String?,
)
