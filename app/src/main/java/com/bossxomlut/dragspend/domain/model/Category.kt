package com.bossxomlut.dragspend.domain.model

data class Category(
    val id: String,
    val userId: String,
    val name: String,
    val icon: String,
    val color: String,
    val type: TransactionType,
    val language: String = "",
    val createdAt: String? = null,
)
