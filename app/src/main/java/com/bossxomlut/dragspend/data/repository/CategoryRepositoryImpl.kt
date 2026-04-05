package com.bossxomlut.dragspend.data.repository

import com.bossxomlut.dragspend.data.local.dao.CategoryDao
import com.bossxomlut.dragspend.data.local.entity.CategoryEntity
import com.bossxomlut.dragspend.data.local.toDomain
import com.bossxomlut.dragspend.data.local.toEntity
import com.bossxomlut.dragspend.data.model.CategoryDto
import com.bossxomlut.dragspend.data.model.toDomain
import com.bossxomlut.dragspend.data.model.toDto
import com.bossxomlut.dragspend.domain.error.mapToAppError
import com.bossxomlut.dragspend.domain.model.Category
import com.bossxomlut.dragspend.domain.model.TransactionType
import com.bossxomlut.dragspend.domain.repository.CategoryRepository
import com.bossxomlut.dragspend.domain.repository.SessionRepository
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.logResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import java.util.UUID

class CategoryRepositoryImpl(
    private val supabase: SupabaseClient,
    private val categoryDao: CategoryDao,
    private val sessionRepository: SessionRepository,
) : CategoryRepository {

    override suspend fun getCategories(userId: String): Result<List<Category>> = runCatching {
        AppLog.d(AppLog.Feature.CATEGORY, "getCategories", "userId=${userId.take(8)}")
        categoryDao.getAll(userId).map { it.toDomain() }
    }.logResult(AppLog.Feature.CATEGORY, "getCategories") { "${it.size} categories" }
        .mapToAppError()

    override suspend fun createCategory(
        userId: String,
        name: String,
        icon: String,
        color: String,
        type: TransactionType,
        language: String,
    ): Result<Category> = runCatching {
        AppLog.d(AppLog.Feature.CATEGORY, "createCategory", "name=$name, type=$type")
        val localId = UUID.randomUUID().toString()
        val now = Instant.now().toString()

        if (!sessionRepository.isAuthenticated()) {
            val entity = CategoryEntity(
                id = localId, userId = userId, name = name, icon = icon,
                color = color, type = type.name.lowercase(), language = language,
                createdAt = now, syncedAt = null, deletedAt = null,
            )
            categoryDao.upsert(entity)
            return@runCatching entity.toDomain()
        }

        val row = mapOf(
            "id" to localId,
            "user_id" to userId,
            "name" to name,
            "icon" to icon,
            "color" to color,
            "type" to type.name.lowercase(),
            "language" to language,
        )
        val created = supabase.from("categories")
            .insert(row) { select() }
            .decodeSingle<CategoryDto>()
            .toDomain()
        categoryDao.upsert(created.toEntity(syncedAt = now))
        created
    }.logResult(AppLog.Feature.CATEGORY, "createCategory") { "id=${it.id}" }
        .mapToAppError()

    override suspend fun updateCategory(category: Category): Result<Category> = runCatching {
        AppLog.d(AppLog.Feature.CATEGORY, "updateCategory", "id=${category.id}, name=${category.name}")

        if (!sessionRepository.isAuthenticated()) {
            val existing = categoryDao.getById(category.id)
            val entity = existing?.copy(
                name = category.name, icon = category.icon,
                color = category.color, type = category.type.name.lowercase(),
            ) ?: category.toEntity()
            categoryDao.upsert(entity)
            return@runCatching category
        }

        val row = mapOf(
            "name" to category.name,
            "icon" to category.icon,
            "color" to category.color,
            "type" to category.type.toDto().name.lowercase(),
        )
        val updated = supabase.from("categories")
            .update(row) {
                filter { eq("id", category.id) }
                select()
            }
            .decodeSingle<CategoryDto>()
            .toDomain()
        categoryDao.upsert(updated.toEntity(syncedAt = Instant.now().toString()))
        updated
    }.logResult(AppLog.Feature.CATEGORY, "updateCategory") { "id=${it.id}" }
        .mapToAppError()

    override suspend fun deleteCategory(categoryId: String): Result<Unit> = runCatching {
        AppLog.d(AppLog.Feature.CATEGORY, "deleteCategory", "id=$categoryId")
        categoryDao.deleteById(categoryId)
        if (sessionRepository.isAuthenticated()) {
            supabase.from("categories").delete { filter { eq("id", categoryId) } }
        }
        Unit
    }.logResult(AppLog.Feature.CATEGORY, "deleteCategory") { "deleted" }
        .mapToAppError()
}
