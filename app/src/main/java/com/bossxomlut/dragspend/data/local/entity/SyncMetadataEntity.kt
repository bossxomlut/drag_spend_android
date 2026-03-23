package com.bossxomlut.dragspend.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks the last sync timestamp for each table.
 * Used to fetch only changed records from the server (delta sync).
 */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    /**
     * The table name: "transactions", "categories", "spending_cards", "card_variants"
     */
    @PrimaryKey
    @ColumnInfo(name = "table_name")
    val tableName: String,

    /**
     * Last time we successfully synced this table (ISO timestamp)
     */
    @ColumnInfo(name = "last_sync_at")
    val lastSyncAt: String,

    /**
     * The maximum server updated_at value we've seen for this table.
     * Used for pull sync to fetch only newer records.
     */
    @ColumnInfo(name = "last_server_timestamp")
    val lastServerTimestamp: String? = null,
)
