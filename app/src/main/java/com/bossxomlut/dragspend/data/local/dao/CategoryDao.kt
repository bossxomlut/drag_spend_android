package com.bossxomlut.dragspend.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bossxomlut.dragspend.data.local.entity.CategoryEntity

@Dao
interface CategoryDao {

    @Query(
        """
        SELECT * FROM categories
        WHERE user_id = :userId AND deleted_at IS NULL
        ORDER BY name ASC
        """,
    )
    suspend fun getAll(userId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): CategoryEntity?

    @Upsert
    suspend fun upsert(vararg entities: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM categories WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)
}
