package com.bossxomlut.dragspend.domain.model

data class Profile(
    val id: String,
    val name: String?,
    val currency: String,
    val language: String?,
    val isSeeded: Boolean,
    val deletedAt: String?,
    val createdAt: String?,
)
