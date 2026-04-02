package com.bossxomlut.dragspend.domain.usecase.category

import com.bossxomlut.dragspend.domain.error.AppError
import com.bossxomlut.dragspend.domain.model.Category
import com.bossxomlut.dragspend.domain.model.TransactionType
import com.bossxomlut.dragspend.domain.repository.CategoryRepository
import com.bossxomlut.dragspend.domain.repository.SessionRepository

class CreateCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(
        name: String,
        icon: String,
        color: String,
        type: TransactionType,
        language: String,
    ): Result<Category> {
        val userId = sessionRepository.getCurrentUserId()
            ?: return Result.failure(AppError.Unauthorized)
        return categoryRepository.createCategory(userId, name, icon, color, type, language)
    }
}
