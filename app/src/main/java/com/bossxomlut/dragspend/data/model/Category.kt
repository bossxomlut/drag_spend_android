package com.bossxomlut.dragspend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val icon: String,
    val color: String,
    val type: TransactionType,
    val language: String = "vi",
    @SerialName("created_at") val createdAt: String? = null,
)
