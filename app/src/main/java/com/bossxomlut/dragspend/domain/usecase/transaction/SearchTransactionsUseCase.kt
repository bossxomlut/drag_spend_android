package com.bossxomlut.dragspend.domain.usecase.transaction

import com.bossxomlut.dragspend.domain.model.Transaction
import com.bossxomlut.dragspend.domain.repository.SessionRepository
import com.bossxomlut.dragspend.domain.repository.TransactionRepository

class SearchTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(
        query: String,
        categoryIds: Set<String>,
        startDate: String?,
        endDate: String?,
    ): Result<List<Transaction>> {
        val userId = sessionRepository.getLocalUserId()
        return transactionRepository.searchTransactions(userId, query, categoryIds, startDate, endDate)
    }
}
