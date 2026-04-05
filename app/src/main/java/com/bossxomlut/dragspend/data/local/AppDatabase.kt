package com.bossxomlut.dragspend.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun cardDao(): CardDao
    abstract fun cardVariantDao(): CardVariantDao

    companion object {
        /** v1 → v2: add categories.updated_at for newer-wins conflict resolution. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN updated_at TEXT")
            }
        }
    }
}
