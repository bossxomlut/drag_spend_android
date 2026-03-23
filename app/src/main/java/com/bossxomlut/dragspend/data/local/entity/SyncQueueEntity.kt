package com.bossxomlut.dragspend.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a pending sync operation in the queue.
 * Operations are processed in FIFO order when connectivity is available.
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * The type of operation: "INSERT", "UPDATE", "DELETE"
     */
    val operation: String,

    /**
     * The target table: "transactions", "categories", "spending_cards", "card_variants"
     */
    @ColumnInfo(name = "table_name")
    val tableName: String,

    /**
     * UUID of the record being synced
     */
    @ColumnInfo(name = "record_id")
    val recordId: String,

    /**
     * JSON snapshot of the record at the time of the change.
     * Used for INSERT and UPDATE operations.
     */
    val payload: String? = null,

    /**
     * When the operation was queued (epoch millis)
     */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Number of failed sync attempts
     */
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    /**
     * Error message from the last failed attempt
     */
    @ColumnInfo(name = "last_error")
    val lastError: String? = null,
) {
    companion object {
        const val OPERATION_INSERT = "INSERT"
        const val OPERATION_UPDATE = "UPDATE"
        const val OPERATION_DELETE = "DELETE"

        const val TABLE_TRANSACTIONS = "transactions"
        const val TABLE_CATEGORIES = "categories"
        const val TABLE_SPENDING_CARDS = "spending_cards"
        const val TABLE_CARD_VARIANTS = "card_variants"
    }
}
