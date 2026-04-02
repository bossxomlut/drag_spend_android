package com.bossxomlut.dragspend.domain.usecase.category

import com.bossxomlut.dragspend.domain.error.AppError
import com.bossxomlut.dragspend.domain.model.Category
import com.bossxomlut.dragspend.domain.repository.CategoryRepository
import com.bossxomlut.dragspend.domain.repository.SessionRepository

class GetCategoriesUseCase(
    private val categoryRepository: CategoryRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(): Result<List<Category>> {
        val userId = sessionRepository.getCurrentUserId()
            ?: return Result.failure(AppError.Unauthorized)
        return categoryRepository.getCategories(userId)
    }
}
