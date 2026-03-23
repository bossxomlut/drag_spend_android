package com.bossxomlut.dragspend.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bossxomlut.dragspend.data.model.CardVariant
import com.bossxomlut.dragspend.data.model.Category
import com.bossxomlut.dragspend.data.model.SpendingCard
import com.bossxomlut.dragspend.data.model.TransactionType

@Entity(tableName = "spending_cards")
data class SpendingCardEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    val title: String,

    @ColumnInfo(name = "category_id")
    val categoryId: String? = null,

    val type: String, // "income" or "expense"

    val note: String? = null,

    val position: Int = 0,

    @ColumnInfo(name = "use_count")
    val useCount: Int = 0,

    val language: String = "vi",

    @ColumnInfo(name = "created_at")
    val createdAt: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,

    // Sync metadata
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.SYNCED,

    @ColumnInfo(name = "local_created_at")
    val localCreatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "server_updated_at")
    val serverUpdatedAt: String? = null,
) {
    fun toDomain(
        variants: List<CardVariant> = emptyList(),
        category: Category? = null,
    ): SpendingCard = SpendingCard(
        id = id,
        userId = userId,
        title = title,
        categoryId = categoryId,
        type = if (type == "income") TransactionType.INCOME else TransactionType.EXPENSE,
        note = note,
        position = position,
        useCount = useCount,
        language = language,
        createdAt = createdAt,
        updatedAt = updatedAt,
        category = category,
        variants = variants,
    )

    companion object {
        fun fromDomain(
            card: SpendingCard,
            syncStatus: SyncStatus = SyncStatus.SYNCED,
            serverUpdatedAt: String? = null,
        ): SpendingCardEntity = SpendingCardEntity(
            id = card.id,
            userId = card.userId,
            title = card.title,
            categoryId = card.categoryId,
            type = card.type.name.lowercase(),
            note = card.note,
            position = card.position,
            useCount = card.useCount,
            language = card.language,
            createdAt = card.createdAt,
            updatedAt = card.updatedAt,
            syncStatus = syncStatus,
            serverUpdatedAt = serverUpdatedAt ?: card.updatedAt,
        )
    }
}
