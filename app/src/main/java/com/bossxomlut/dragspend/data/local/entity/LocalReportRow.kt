package com.bossxomlut.dragspend.data.local.entity

import androidx.room.ColumnInfo

/** POJO kết quả cho query tổng hợp monthly report từ Room. */
data class LocalReportRow(
    val date: String,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    @ColumnInfo(name = "category_name") val categoryName: String?,
    @ColumnInfo(name = "category_icon") val categoryIcon: String?,
    @ColumnInfo(name = "category_color") val categoryColor: String?,
    val type: String,
    val total: Long,
)
