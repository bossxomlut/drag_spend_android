package com.bossxomlut.dragspend.domain.usecase.transaction

import com.bossxomlut.dragspend.domain.model.Transaction
import com.bossxomlut.dragspend.domain.repository.TransactionRepository
import com.bossxomlut.dragspend.domain.repository.UpdateTransactionRequest

class UpdateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        transactionId: String,
        request: UpdateTransactionRequest,
    ): Result<Transaction> = transactionRepository.updateTransaction(transactionId, request)
}
