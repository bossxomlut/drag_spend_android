package com.bossxomlut.dragspend.data.repository

import com.bossxomlut.dragspend.data.model.Category
import com.bossxomlut.dragspend.data.model.TransactionType
import com.bossxomlut.dragspend.domain.repository.CategoryRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class CategoryRepositoryImpl(
    private val supabase: SupabaseClient,
) : CategoryRepository {

    override suspend fun getCategories(userId: String): Result<List<Category>> = runCatching {
        supabase.from("categories")
            .select {
                filter { eq("user_id", userId) }
                order("name", order = Order.ASCENDING)
            }
            .decodeList<Category>()
    }

    override suspend fun createCategory(
        userId: String,
        name: String,
        icon: String,
        color: String,
        type: TransactionType,
        language: String,
    ): Result<Category> = runCatching {
        val row = mapOf(
            "user_id" to userId,
            "name" to name,
            "icon" to icon,
            "color" to color,
            "type" to type.name.lowercase(),
            "language" to language,
        )
        supabase.from("categories")
            .insert(row) { select() }
            .decodeSingle<Category>()
    }

    override suspend fun updateCategory(category: Category): Result<Category> = runCatching {
        val row = mapOf(
            "name" to category.name,
            "icon" to category.icon,
            "color" to category.color,
            "type" to category.type.name.lowercase(),
        )
        supabase.from("categories")
            .update(row) {
                filter { eq("id", category.id) }
                select()
            }
            .decodeSingle<Category>()
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> = runCatching {
        supabase.from("categories")
            .delete {
                filter { eq("id", categoryId) }
            }
        Unit
    }
}
