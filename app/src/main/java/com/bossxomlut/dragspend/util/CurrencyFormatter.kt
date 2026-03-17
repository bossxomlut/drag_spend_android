package com.bossxomlut.dragspend.util

import kotlin.math.abs
import kotlin.math.roundToLong

object CurrencyFormatter {

    fun formatCompact(amount: Long): String {
        val absAmount = abs(amount)
        val prefix = if (amount < 0) "-" else ""
        return when {
            absAmount >= 1_000_000_000L -> {
                val value = absAmount / 1_000_000_000.0
                "$prefix${formatDecimal(value)}b"
            }
            absAmount >= 1_000_000L -> {
                val value = absAmount / 1_000_000.0
                "$prefix${formatDecimal(value)}m"
            }
            absAmount >= 1_000L -> {
                val value = absAmount / 1_000.0
                "$prefix${formatDecimal(value)}k"
            }
            else -> "$prefix$absAmount"
        }
    }

    private fun formatDecimal(value: Double): String {
        val rounded = (value * 10).roundToLong() / 10.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    fun parseCompact(input: String): Long? {
        val trimmed = input.trim().lowercase()
        if (trimmed.isEmpty()) return null
        return try {
            when {
                trimmed.endsWith("b") -> {
                    (trimmed.dropLast(1).toDouble() * 1_000_000_000).roundToLong()
                }
                trimmed.endsWith("m") -> {
                    (trimmed.dropLast(1).toDouble() * 1_000_000).roundToLong()
                }
                trimmed.endsWith("k") -> {
                    (trimmed.dropLast(1).toDouble() * 1_000).roundToLong()
                }
                else -> trimmed.toLong()
            }
        } catch (e: NumberFormatException) {
            null
        }
    }

    fun isValidAmountInput(input: String): Boolean =
        input.matches(Regex("^\\d+(\\.\\d+)?(k|m|b)?$", RegexOption.IGNORE_CASE))
}
