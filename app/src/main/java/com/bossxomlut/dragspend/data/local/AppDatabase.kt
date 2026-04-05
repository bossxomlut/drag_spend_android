package com.bossxomlut.dragspend.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bossxomlut.dragspend.data.local.dao.CardDao
import com.bossxomlut.dragspend.data.local.dao.CardVariantDao
import com.bossxomlut.dragspend.data.local.dao.CategoryDao
import com.bossxomlut.dragspend.data.local.dao.TransactionDao
import com.bossxomlut.dragspend.data.local.entity.CardVariantEntity
import com.bossxomlut.dragspend.data.local.entity.CategoryEntity
import com.bossxomlut.dragspend.data.local.entity.SpendingCardEntity
import com.bossxomlut.dragspend.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        SpendingCardEntity::class,
        CardVariantEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun cardDao(): CardDao
    abstract fun cardVariantDao(): CardVariantDao
}
