package com.bossxomlut.dragspend.data.repository

import com.bossxomlut.dragspend.data.model.CategoryDto
import com.bossxomlut.dragspend.data.model.toDomain
import com.bossxomlut.dragspend.data.model.toDto
import com.bossxomlut.dragspend.domain.error.mapToAppError
import com.bossxomlut.dragspend.domain.model.Category
import com.bossxomlut.dragspend.domain.model.TransactionType
import com.bossxomlut.dragspend.domain.repository.CategoryRepository
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.logResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class CategoryRepositoryImpl(
    private val supabase: SupabaseClient,
) : CategoryRepository {

    /** Session-scoped cache: userId → sorted category list. */
    private val cache = mutableMapOf<String, List<Category>>()

    override suspend fun getCategories(userId: String): Result<List<Category>> {
        cache[userId]?.let { cached ->
            AppLog.d(AppLog.Feature.CATEGORY, "getCategories", "cache hit, ${cached.size} categories")
            return Result.success(cached)
        }
        return runCatching {
            AppLog.d(AppLog.Feature.CATEGORY, "getCategories", "userId=${userId.take(8)}")
            supabase.from("categories")
                .select {
                    filter { eq("user_id", userId) }
                    order("name", order = Order.ASCENDING)
                }
                .decodeList<CategoryDto>()
                .map { it.toDomain() }
                .also { cache[userId] = it }
        }.logResult(AppLog.Feature.CATEGORY, "getCategories") { "${it.size} categories" }
            .mapToAppError()
    }

    override suspend fun createCategory(
        userId: String,
        name: String,
        icon: String,
        color: String,
        type: TransactionType,
        language: String,
    ): Result<Category> = runCatching {
        AppLog.d(AppLog.Feature.CATEGORY, "createCategory", "name=$name, type=$type")
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
            .decodeSingle<CategoryDto>()
            .toDomain()
            .also { created ->
                cache[userId] = ((cache[userId] ?: emptyList()) + created)
                    .sortedBy { it.name }
            }
    }.logResult(AppLog.Feature.CATEGORY, "createCategory") { "id=${it.id}" }
        .mapToAppError()

    override suspend fun updateCategory(category: Category): Result<Category> = runCatching {
        AppLog.d(AppLog.Feature.CATEGORY, "updateCategory", "id=${category.id}, name=${category.name}")
        val row = mapOf(
            "name" to category.name,
            "icon" to category.icon,
            "color" to category.color,
            "type" to category.type.toDto().name.lowercase(),
        )
        supabase.from("categories")
            .update(row) {
                filter { eq("id", category.id) }
                select()
            }
            .decodeSingle<CategoryDto>()
            .toDomain()
            .also { updated ->
                val userId = updated.userId
                cache[userId] = ((cache[userId] ?: emptyList())
                    .filterNot { it.id == updated.id } + updated)
                    .sortedBy { it.name }
            }
    }.logResult(AppLog.Feature.CATEGORY, "updateCategory") { "id=${it.id}" }
        .mapToAppError()

    override suspend fun deleteCategory(categoryId: String): Result<Unit> = runCatching {
        AppLog.d(AppLog.Feature.CATEGORY, "deleteCategory", "id=$categoryId")
        // Remove from cache before the network call so UI updates feel instant.
        cache.forEach { (userId, list) ->
            if (list.any { it.id == categoryId }) {
                cache[userId] = list.filterNot { it.id == categoryId }
            }
        }
        supabase.from("categories")
            .delete {
                filter { eq("id", categoryId) }
            }
        Unit
    }.logResult(AppLog.Feature.CATEGORY, "deleteCategory") { "deleted" }
        .mapToAppError()
}
