package com.bossxomlut.dragspend.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bossxomlut.dragspend.data.local.entity.LocalReportRow
import com.bossxomlut.dragspend.data.local.entity.TransactionEntity

@Dao
interface TransactionDao {

    /** Returns ALL transactions for a user including soft-deleted — used for userId migration. */
    @Query("SELECT * FROM transactions WHERE user_id = :userId")
    suspend fun getAll(userId: String): List<TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE user_id = :userId AND date = :date AND deleted_at IS NULL
        ORDER BY position ASC
        """,
    )
    suspend fun getByDate(userId: String, date: String): List<TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE user_id = :userId
            AND date >= :startDate AND date < :endDate
            AND deleted_at IS NULL
        ORDER BY date ASC, position ASC
        """,
    )
    suspend fun getByDateRange(
        userId: String,
        startDate: String,
        endDate: String,
    ): List<TransactionEntity>

    @Query(
        """
        SELECT MAX(position) FROM transactions
        WHERE user_id = :userId AND date = :date AND deleted_at IS NULL
        """,
    )
    suspend fun getMaxPositionForDate(userId: String, date: String): Int?

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Query(
        """
        SELECT * FROM transactions
        WHERE user_id = :userId
            AND deleted_at IS NULL
            AND title LIKE '%' || :query || '%'
            AND (:startDate IS NULL OR date >= :startDate)
            AND (:endDate IS NULL OR date <= :endDate)
        ORDER BY date DESC, position ASC
        LIMIT 500
        """,
    )
    suspend fun search(
        userId: String,
        query: String,
        startDate: String?,
        endDate: String?,
    ): List<TransactionEntity>

    @Query(
        """
        SELECT
            t.date,
            t.category_id AS category_id,
            c.name AS category_name,
            c.icon AS category_icon,
            c.color AS category_color,
            t.type,
            SUM(t.amount) AS total
        FROM transactions t
        LEFT JOIN categories c ON t.category_id = c.id
        WHERE t.user_id = :userId
            AND t.date >= :startDate AND t.date < :endDate
            AND t.deleted_at IS NULL
        GROUP BY t.date, t.category_id, t.type
        ORDER BY t.date ASC
        """,
    )
    suspend fun getMonthlyReport(
        userId: String,
        startDate: String,
        endDate: String,
    ): List<LocalReportRow>

    @Upsert
    suspend fun upsert(vararg entities: TransactionEntity)

    @Query("UPDATE transactions SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: String)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)
}
