package com.bossxomlut.dragspend.data.model

import com.bossxomlut.dragspend.domain.model.CardVariant
import com.bossxomlut.dragspend.domain.model.Category
import com.bossxomlut.dragspend.domain.model.Profile
import com.bossxomlut.dragspend.domain.model.ReportEntry
import com.bossxomlut.dragspend.domain.model.SpendingCard
import com.bossxomlut.dragspend.domain.model.Transaction
import com.bossxomlut.dragspend.domain.model.TransactionType

fun TransactionTypeDto.toDomain(): TransactionType = when (this) {
    TransactionTypeDto.INCOME -> TransactionType.INCOME
    TransactionTypeDto.EXPENSE -> TransactionType.EXPENSE
}

fun TransactionType.toDto(): TransactionTypeDto = when (this) {
    TransactionType.INCOME -> TransactionTypeDto.INCOME
    TransactionType.EXPENSE -> TransactionTypeDto.EXPENSE
}

fun CategoryDto.toDomain(): Category = Category(
    id = id,
    userId = userId,
    name = name,
    icon = icon,
    color = color,
    type = type.toDomain(),
    language = language,
    createdAt = createdAt,
)

fun ProfileDto.toDomain(): Profile = Profile(
    id = id,
    name = name,
    currency = currency,
    language = language,
    isSeeded = isSeeded,
    deletedAt = deletedAt,
    createdAt = createdAt,
)

fun CardVariantDto.toDomain(): CardVariant = CardVariant(
    id = id,
    cardId = cardId,
    label = label,
    amount = amount,
    isDefault = isDefault ?: false,
    position = position ?: 0,
    createdAt = createdAt,
)

fun SpendingCardDto.toDomain(
    category: Category?,
    variants: List<CardVariant>,
): SpendingCard = SpendingCard(
    id = id,
    userId = userId,
    title = title,
    categoryId = categoryId,
    type = type.toDomain(),
    note = note,
    position = position ?: 0,
    useCount = useCount,
    language = language,
    createdAt = createdAt,
    updatedAt = updatedAt,
    category = category,
    variants = variants,
)

fun TransactionDto.toDomain(category: Category? = null): Transaction = Transaction(
    id = id,
    userId = userId,
    sourceCardId = sourceCardId,
    date = date,
    title = title,
    amount = amount,
    categoryId = categoryId,
    type = type.toDomain(),
    note = note,
    position = position ?: 0,
    createdAt = createdAt,
    updatedAt = updatedAt,
    category = category,
)

fun MonthlyReportRowDto.toDomain(): ReportEntry = ReportEntry(
    date = date,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    type = type.toDomain(),
    total = total,
)
