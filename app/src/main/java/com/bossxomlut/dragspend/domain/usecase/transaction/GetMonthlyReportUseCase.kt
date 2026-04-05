package com.bossxomlut.dragspend.domain.usecase.transaction

import com.bossxomlut.dragspend.domain.model.ReportEntry
import com.bossxomlut.dragspend.domain.repository.SessionRepository
import com.bossxomlut.dragspend.domain.repository.TransactionRepository

class GetMonthlyReportUseCase(
    private val transactionRepository: TransactionRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(yearMonth: String): Result<List<ReportEntry>> {
        val userId = sessionRepository.getLocalUserId()
        return transactionRepository.getMonthlyReport(userId, yearMonth)
    }
}
