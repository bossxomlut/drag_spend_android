package com.bossxomlut.dragspend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val icon: String,
    val color: String,
    val type: TransactionTypeDto,
    val language: String = "vi",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
