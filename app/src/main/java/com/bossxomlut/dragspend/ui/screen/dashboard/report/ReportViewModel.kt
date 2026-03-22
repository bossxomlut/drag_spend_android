package com.bossxomlut.dragspend.ui.screen.dashboard.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bossxomlut.dragspend.data.model.MonthlyReportRow
import com.bossxomlut.dragspend.data.model.TransactionType
import com.bossxomlut.dragspend.domain.repository.TransactionRepository
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.toFriendlyMessage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import java.time.YearMonth
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
    val categoryId: String?,
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

    /**
     * In-memory cache: yearMonth ("yyyy-MM") → processed ReportUiState.
     * Switching back to a previously loaded month is instant (no network call).
     */
    private val monthCache = mutableMapOf<String, ReportUiState>()

    fun loadReport(yearMonth: String) {
        val userId = currentUserId ?: return
        AppLog.d(AppLog.Feature.REPORT, "loadReport", "userId=${userId.take(8)}, yearMonth=$yearMonth")

        // Cache hit → show immediately, no network call
        val cached = monthCache[yearMonth]
        if (cached != null) {
            AppLog.d(AppLog.Feature.REPORT, "loadReport", "cache hit for $yearMonth")
            _uiState.value = cached
            return
        }

        // Cache miss → keep current data visible while loading (no blank screen)
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            transactionRepository.getMonthlyReport(userId, yearMonth)
                .onSuccess { rows ->
                    processReport(rows, yearMonth)
                    // Store processed state in cache
                    monthCache[yearMonth] = _uiState.value
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.toFriendlyMessage()) } }
        }
    }

    private fun processReport(rows: List<MonthlyReportRow>, yearMonth: String) {
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
                    categoryId = row.categoryId,
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

        // Fill all days of the month so the bar chart always shows a complete grid
        val daysInMonth = YearMonth.parse(yearMonth).lengthOfMonth()
        val dailyBars = (1..daysInMonth)
            .map { day ->
                val pair = dailyMap[day] ?: (0L to 0L)
                DailyBarData(day = day, expense = pair.first, income = pair.second)
            }

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
        AppLog.success(
            AppLog.Feature.REPORT,
            "processReport",
            "expense=$totalExpense, income=$totalIncome, days=${dailyBars.size}, categories=${categorySlices.size}",
        )
    }

    /**
     * Removes the cached report for [yearMonth] and fetches fresh data.
     * Called by ReportScreen when the Today screen signals that transactions were mutated.
     */
    fun invalidateAndReload(yearMonth: String) {
        AppLog.d(AppLog.Feature.REPORT, "invalidateAndReload", "yearMonth=$yearMonth")
        monthCache.remove(yearMonth)
        loadReport(yearMonth)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
