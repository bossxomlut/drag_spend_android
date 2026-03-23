package com.bossxomlut.dragspend.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bossxomlut.dragspend.data.model.Category
import com.bossxomlut.dragspend.data.model.Transaction
import com.bossxomlut.dragspend.data.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "source_card_id")
    val sourceCardId: String? = null,

    val date: String,

    val title: String,

    val amount: Long,

    @ColumnInfo(name = "category_id")
    val categoryId: String? = null,

    val type: String, // "income" or "expense"

    val note: String? = null,

    val position: Int = 0,

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
    fun toDomain(category: Category? = null): Transaction = Transaction(
        id = id,
        userId = userId,
        sourceCardId = sourceCardId,
        date = date,
        title = title,
        amount = amount,
        categoryId = categoryId,
        type = if (type == "income") TransactionType.INCOME else TransactionType.EXPENSE,
        note = note,
        position = position,
        createdAt = createdAt,
        updatedAt = updatedAt,
        category = category,
        syncStatus = syncStatus,
    )

    companion object {
        fun fromDomain(
            transaction: Transaction,
            syncStatus: SyncStatus = SyncStatus.SYNCED,
            serverUpdatedAt: String? = null,
        ): TransactionEntity = TransactionEntity(
            id = transaction.id,
            userId = transaction.userId,
            sourceCardId = transaction.sourceCardId,
            date = transaction.date,
            title = transaction.title,
            amount = transaction.amount,
            categoryId = transaction.categoryId,
            type = transaction.type.name.lowercase(),
            note = transaction.note,
            position = transaction.position,
            createdAt = transaction.createdAt,
            updatedAt = transaction.updatedAt,
            syncStatus = syncStatus,
            serverUpdatedAt = serverUpdatedAt ?: transaction.updatedAt,
        )
    }
}
