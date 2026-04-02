package com.bossxomlut.dragspend.domain.usecase.transaction

import com.bossxomlut.dragspend.domain.error.AppError
import com.bossxomlut.dragspend.domain.model.Transaction
import com.bossxomlut.dragspend.domain.repository.SessionRepository
import com.bossxomlut.dragspend.domain.repository.TransactionRepository

class CopyFromYesterdayUseCase(
    private val transactionRepository: TransactionRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(fromDate: String, toDate: String): Result<List<Transaction>> {
        val userId = sessionRepository.getCurrentUserId()
            ?: return Result.failure(AppError.Unauthorized)
        return transactionRepository.copyFromYesterday(userId, fromDate, toDate)
    }
}
