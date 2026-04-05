package com.bossxomlut.dragspend.domain.usecase.transaction

import com.bossxomlut.dragspend.domain.model.Transaction
import com.bossxomlut.dragspend.domain.repository.SessionRepository
import com.bossxomlut.dragspend.domain.repository.TransactionRepository

class GetDailyTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(date: String): Result<List<Transaction>> {
        val userId = sessionRepository.getLocalUserId()
        return transactionRepository.getTransactions(userId, date)
    }
}
