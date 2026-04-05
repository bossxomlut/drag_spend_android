package com.bossxomlut.dragspend.data.local

import com.bossxomlut.dragspend.data.local.entity.CardVariantEntity
import com.bossxomlut.dragspend.data.local.entity.CategoryEntity
import com.bossxomlut.dragspend.data.local.entity.LocalReportRow
import com.bossxomlut.dragspend.data.local.entity.SpendingCardEntity
import com.bossxomlut.dragspend.data.local.entity.TransactionEntity
import com.bossxomlut.dragspend.data.model.CardVariantDto
import com.bossxomlut.dragspend.data.model.CategoryDto
import com.bossxomlut.dragspend.data.model.SpendingCardDto
import com.bossxomlut.dragspend.data.model.TransactionDto
import com.bossxomlut.dragspend.domain.model.CardVariant
import com.bossxomlut.dragspend.domain.model.Category
import com.bossxomlut.dragspend.domain.model.ReportEntry
import com.bossxomlut.dragspend.domain.model.SpendingCard
import com.bossxomlut.dragspend.domain.model.Transaction
import com.bossxomlut.dragspend.domain.model.TransactionType

// ── Domain → Entity ─────────────────────────────────────────────────────────

fun Transaction.toEntity(syncedAt: String? = null): TransactionEntity = TransactionEntity(
    id = id,
    userId = userId,
    sourceCardId = sourceCardId,
    date = date,
    title = title,
    amount = amount,
    categoryId = categoryId,
    type = type.name.lowercase(),
    note = note,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncedAt = syncedAt,
    deletedAt = null,
)

fun Category.toEntity(syncedAt: String? = null): CategoryEntity = CategoryEntity(
    id = id,
    userId = userId,
    name = name,
    icon = icon,
    color = color,
    type = type.name.lowercase(),
    language = language,
    createdAt = createdAt,
    syncedAt = syncedAt,
    deletedAt = null,
)

fun CardVariant.toEntity(syncedAt: String? = null): CardVariantEntity = CardVariantEntity(
    id = id,
    cardId = cardId,
    label = label,
    amount = amount,
    isDefault = isDefault,
    position = position,
    createdAt = createdAt,
    syncedAt = syncedAt,
)

fun SpendingCard.toEntity(syncedAt: String? = null): SpendingCardEntity = SpendingCardEntity(
    id = id,
    userId = userId,
    title = title,
    categoryId = categoryId,
    type = type.name.lowercase(),
    note = note,
    position = position,
    useCount = useCount,
    language = language,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncedAt = syncedAt,
    deletedAt = null,
)

// ── Entity → Domain ─────────────────────────────────────────────────────────

fun TransactionEntity.toDomain(category: Category? = null): Transaction = Transaction(
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
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    userId = userId,
    name = name,
    icon = icon,
    color = color,
    type = if (type == "income") TransactionType.INCOME else TransactionType.EXPENSE,
    language = language,
    createdAt = createdAt,
)

fun CardVariantEntity.toDomain(): CardVariant = CardVariant(
    id = id,
    cardId = cardId,
    label = label,
    amount = amount,
    isDefault = isDefault,
    position = position,
    createdAt = createdAt,
)

fun SpendingCardEntity.toDomain(
    category: Category?,
    variants: List<CardVariant>,
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

fun LocalReportRow.toDomain(): ReportEntry = ReportEntry(
    date = date,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    type = if (type == "income") TransactionType.INCOME else TransactionType.EXPENSE,
    total = total,
)

// ── DTO → Entity (cho sync từ Supabase → Room) ───────────────────────────────

fun TransactionDto.toEntity(syncedAt: String? = null): TransactionEntity = TransactionEntity(
    id = id,
    userId = userId,
    sourceCardId = sourceCardId,
    date = date,
    title = title,
    amount = amount,
    categoryId = categoryId,
    type = type.name.lowercase(),
    note = note,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncedAt = syncedAt,
    deletedAt = null,
)

fun CategoryDto.toEntity(syncedAt: String? = null): CategoryEntity = CategoryEntity(
    id = id,
    userId = userId,
    name = name,
    icon = icon,
    color = color,
    type = type.name.lowercase(),
    language = language,
    createdAt = createdAt,
    syncedAt = syncedAt,
    deletedAt = null,
)

fun SpendingCardDto.toEntity(syncedAt: String? = null): SpendingCardEntity = SpendingCardEntity(
    id = id,
    userId = userId,
    title = title,
    categoryId = categoryId,
    type = type.name.lowercase(),
    note = note,
    position = position,
    useCount = useCount,
    language = language,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncedAt = syncedAt,
    deletedAt = null,
)

fun CardVariantDto.toEntity(syncedAt: String? = null): CardVariantEntity = CardVariantEntity(
    id = id,
    cardId = cardId,
    label = label,
    amount = amount,
    isDefault = isDefault,
    position = position,
    createdAt = createdAt,
    syncedAt = syncedAt,
)
