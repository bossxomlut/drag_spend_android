package com.bossxomlut.dragspend.domain.usecase.transaction

import com.bossxomlut.dragspend.domain.repository.TransactionRepository

class DeleteTransactionUseCase(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(transactionId: String): Result<Unit> =
        transactionRepository.deleteTransaction(transactionId)
}
