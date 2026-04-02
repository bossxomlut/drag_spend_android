package com.bossxomlut.dragspend.domain.repository

import com.bossxomlut.dragspend.domain.model.Category
import com.bossxomlut.dragspend.domain.model.TransactionType

interface CategoryRepository {
    suspend fun getCategories(userId: String): Result<List<Category>>
    suspend fun createCategory(
        userId: String,
        name: String,
        icon: String,
        color: String,
        type: TransactionType,
        language: String,
    ): Result<Category>
    suspend fun updateCategory(category: Category): Result<Category>
    suspend fun deleteCategory(categoryId: String): Result<Unit>
}
