package com.bossxomlut.dragspend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val name: String? = null,
    val currency: String = "VND",
    val language: String? = null,
    @SerialName("is_seeded") val isSeeded: Boolean = false,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
