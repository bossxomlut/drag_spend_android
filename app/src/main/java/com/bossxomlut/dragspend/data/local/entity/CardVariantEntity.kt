package com.bossxomlut.dragspend.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bossxomlut.dragspend.data.model.CardVariant

@Entity(
    tableName = "card_variants",
    foreignKeys = [
        ForeignKey(
            entity = SpendingCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("card_id")],
)
data class CardVariantEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "card_id")
    val cardId: String,

    val label: String? = null,

    val amount: Long,

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    val position: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: String? = null,

    // Sync metadata
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.SYNCED,

    @ColumnInfo(name = "local_created_at")
    val localCreatedAt: Long = System.currentTimeMillis(),
) {
    fun toDomain(): CardVariant = CardVariant(
        id = id,
        cardId = cardId,
        label = label,
        amount = amount,
        isDefault = isDefault,
        position = position,
        createdAt = createdAt,
    )

    companion object {
        fun fromDomain(
            variant: CardVariant,
            syncStatus: SyncStatus = SyncStatus.SYNCED,
        ): CardVariantEntity = CardVariantEntity(
            id = variant.id,
            cardId = variant.cardId,
            label = variant.label,
            amount = variant.amount,
            isDefault = variant.isDefault,
            position = variant.position,
            createdAt = variant.createdAt,
            syncStatus = syncStatus,
        )
    }
}
