package com.bossxomlut.dragspend.ui.screen.dashboard.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bossxomlut.dragspend.domain.model.ReportEntry
import com.bossxomlut.dragspend.domain.model.TransactionType
import com.bossxomlut.dragspend.domain.usecase.transaction.GetMonthlyReportUseCase
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.toFriendlyMessage
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ChartFilter {
    data object All : ChartFilter()
    data object ExpenseOnly : ChartFilter()
    data object IncomeOnly : ChartFilter()
    data class ByCategoryFilter(
        val categoryId: String,
        val name: String,
        val icon: String,
        val color: String,
    ) : ChartFilter()
}

data class DailyBarData(
    val day: Int,
    val expense: Long,
    val income: Long,
)

data class CategorySlice(
    val categoryId: String?,
    val name: String,
    val icon: String,
    val color: String,
    val amount: Long,
)

data class ReportUiState(
    val isLoading: Boolean = false,
    val dailyBars: List<DailyBarData> = emptyList(),
    val categoryDailyBars: Map<String, List<Long>> = emptyMap(),
    val categorySlices: List<CategorySlice> = emptyList(),
    val totalExpense: Long = 0L,
    val totalIncome: Long = 0L,
    val avgDailyExpense: Long = 0L,
    val highestExpenseDay: Long = 0L,
    val errorMessage: String? = null,
)


class ReportViewModel(
    private val getMonthlyReportUseCase: GetMonthlyReportUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private val monthCache = mutableMapOf<String, ReportUiState>()

    fun loadReport(yearMonth: String) {
        AppLog.d(AppLog.Feature.REPORT, "loadReport", "yearMonth=$yearMonth")

        val cached = monthCache[yearMonth]
        if (cached != null) {
            AppLog.d(AppLog.Feature.REPORT, "loadReport", "cache hit for $yearMonth")
            _uiState.value = cached
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            getMonthlyReportUseCase(yearMonth)
                .onSuccess { entries ->
                    processReport(entries, yearMonth)
                    monthCache[yearMonth] = _uiState.value
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.toFriendlyMessage()) } }
        }
    }

    private fun processReport(entries: List<ReportEntry>, yearMonth: String) {
        val dailyMap = mutableMapOf<Int, Pair<Long, Long>>()
        val categoryMap = mutableMapOf<String, CategorySlice>()
        val categoryDailyMapBuilder = mutableMapOf<String, MutableMap<Int, Long>>()
        var totalExpense = 0L
        var totalIncome = 0L

        entries.forEach { entry ->
            val day = entry.date.substring(8, 10).toIntOrNull() ?: return@forEach
            val current = dailyMap[day] ?: (0L to 0L)
            if (entry.type == TransactionType.EXPENSE) {
                dailyMap[day] = current.copy(first = current.first + entry.total)
                totalExpense += entry.total
                val catKey = entry.categoryId ?: "other"
                val existing = categoryMap[catKey]
                categoryMap[catKey] = CategorySlice(
                    categoryId = entry.categoryId,
                    name = entry.categoryName ?: "Other",
                    icon = entry.categoryIcon ?: "📦",
                    color = entry.categoryColor ?: "#9E9E9E",
                    amount = (existing?.amount ?: 0L) + entry.total,
                )
                val catDayMap = categoryDailyMapBuilder.getOrPut(catKey) { mutableMapOf() }
                catDayMap[day] = (catDayMap[day] ?: 0L) + entry.total
            } else {
                dailyMap[day] = current.copy(second = current.second + entry.total)
                totalIncome += entry.total
            }
        }

        val daysInMonth = YearMonth.parse(yearMonth).lengthOfMonth()
        val dailyBars = (1..daysInMonth).map { day ->
            val pair = dailyMap[day] ?: (0L to 0L)
            DailyBarData(day = day, expense = pair.first, income = pair.second)
        }

        val categoryDailyBars = categoryDailyMapBuilder.mapValues { (_, dayAmounts) ->
            (1..daysInMonth).map { day -> dayAmounts[day] ?: 0L }
        }

        val categorySlices = categoryMap.values.sortedByDescending { it.amount }
        val expenseDays = dailyBars.count { it.expense > 0 }
        val avgDaily = if (expenseDays > 0) totalExpense / expenseDays else 0L
        val highestDay = dailyBars.maxOfOrNull { it.expense } ?: 0L

        _uiState.update {
            it.copy(
                isLoading = false,
                dailyBars = dailyBars,
                categoryDailyBars = categoryDailyBars,
                categorySlices = categorySlices,
                totalExpense = totalExpense,
                totalIncome = totalIncome,
                avgDailyExpense = avgDaily,
                highestExpenseDay = highestDay,
            )
        }
        AppLog.success(
            AppLog.Feature.REPORT,
            "processReport",
            "expense=$totalExpense, income=$totalIncome, days=${dailyBars.size}, categories=${categorySlices.size}",
        )
    }

    fun invalidateAndReload(yearMonth: String) {
        AppLog.d(AppLog.Feature.REPORT, "invalidateAndReload", "yearMonth=$yearMonth")
        monthCache.remove(yearMonth)
        loadReport(yearMonth)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
