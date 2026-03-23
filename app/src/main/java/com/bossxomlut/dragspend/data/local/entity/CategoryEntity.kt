package com.bossxomlut.dragspend.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bossxomlut.dragspend.data.model.Category
import com.bossxomlut.dragspend.data.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    val name: String,

    val icon: String,

    val color: String,

    val type: String, // "income" or "expense"

    val language: String = "vi",

    @ColumnInfo(name = "created_at")
    val createdAt: String? = null,

    // Sync metadata
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.SYNCED,

    @ColumnInfo(name = "local_created_at")
    val localCreatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "server_updated_at")
    val serverUpdatedAt: String? = null,
) {
    fun toDomain(): Category = Category(
        id = id,
        userId = userId,
        name = name,
        icon = icon,
        color = color,
        type = if (type == "income") TransactionType.INCOME else TransactionType.EXPENSE,
        language = language,
        createdAt = createdAt,
    )

    companion object {
        fun fromDomain(
            category: Category,
            syncStatus: SyncStatus = SyncStatus.SYNCED,
            serverUpdatedAt: String? = null,
        ): CategoryEntity = CategoryEntity(
            id = category.id,
            userId = category.userId,
            name = category.name,
            icon = category.icon,
            color = category.color,
            type = category.type.name.lowercase(),
            language = category.language,
            createdAt = category.createdAt,
            syncStatus = syncStatus,
            serverUpdatedAt = serverUpdatedAt ?: category.createdAt,
        )
    }
}
