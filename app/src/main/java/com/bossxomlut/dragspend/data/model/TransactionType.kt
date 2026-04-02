package com.bossxomlut.dragspend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TransactionTypeDto {
    @SerialName("income")
    INCOME,

    @SerialName("expense")
    EXPENSE,
}
