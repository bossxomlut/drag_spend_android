package com.bossxomlut.dragspend.ui.screen.dashboard.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bossxomlut.dragspend.data.model.MonthlyReportRow
import com.bossxomlut.dragspend.data.model.TransactionType
import com.bossxomlut.dragspend.domain.repository.TransactionRepository
import com.bossxomlut.dragspend.util.toFriendlyMessage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DailyBarData(
    val day: Int,
    val expense: Long,
    val income: Long,
)

data class CategorySlice(
    val name: String,
    val icon: String,
    val color: String,
    val amount: Long,
)

data class ReportUiState(
    val isLoading: Boolean = false,
    val dailyBars: List<DailyBarData> = emptyList(),
    val categorySlices: List<CategorySlice> = emptyList(),
    val totalExpense: Long = 0L,
    val totalIncome: Long = 0L,
    val avgDailyExpense: Long = 0L,
    val highestExpenseDay: Long = 0L,
    val errorMessage: String? = null,
)


class ReportViewModel(
    private val supabase: SupabaseClient,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private val currentUserId get() = supabase.auth.currentUserOrNull()?.id

    fun loadReport(yearMonth: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            transactionRepository.getMonthlyReport(userId, yearMonth)
                .onSuccess { rows -> processReport(rows) }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.toFriendlyMessage()) } }
        }
    }

    private fun processReport(rows: List<MonthlyReportRow>) {
        val dailyMap = mutableMapOf<Int, Pair<Long, Long>>()
        val categoryMap = mutableMapOf<String, CategorySlice>()
        var totalExpense = 0L
        var totalIncome = 0L

        rows.forEach { row ->
            val day = row.date.substring(8, 10).toIntOrNull() ?: return@forEach
            val current = dailyMap[day] ?: (0L to 0L)
            if (row.type == TransactionType.EXPENSE) {
                dailyMap[day] = current.copy(first = current.first + row.total)
                totalExpense += row.total
                val catKey = row.categoryId ?: "other"
                val existing = categoryMap[catKey]
                categoryMap[catKey] = CategorySlice(
                    name = row.categoryName ?: "Other",
                    icon = row.categoryIcon ?: "📦",
                    color = row.categoryColor ?: "#9E9E9E",
                    amount = (existing?.amount ?: 0L) + row.total,
                )
            } else {
                dailyMap[day] = current.copy(second = current.second + row.total)
                totalIncome += row.total
            }
        }

        val dailyBars = dailyMap.entries
            .sortedBy { it.key }
            .map { (day, pair) -> DailyBarData(day = day, expense = pair.first, income = pair.second) }

        val categorySlices = categoryMap.values.sortedByDescending { it.amount }

        val expenseDays = dailyBars.count { it.expense > 0 }
        val avgDaily = if (expenseDays > 0) totalExpense / expenseDays else 0L
        val highestDay = dailyBars.maxOfOrNull { it.expense } ?: 0L

        _uiState.update {
            it.copy(
                isLoading = false,
                dailyBars = dailyBars,
                categorySlices = categorySlices,
                totalExpense = totalExpense,
                totalIncome = totalIncome,
                avgDailyExpense = avgDaily,
                highestExpenseDay = highestDay,
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
