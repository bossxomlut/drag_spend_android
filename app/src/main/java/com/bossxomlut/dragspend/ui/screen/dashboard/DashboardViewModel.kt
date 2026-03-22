package com.bossxomlut.dragspend.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bossxomlut.dragspend.data.model.Category
import com.bossxomlut.dragspend.data.model.TransactionType
import com.bossxomlut.dragspend.domain.repository.CategoryRepository
import com.bossxomlut.dragspend.util.AppLog
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class DashboardViewModel(
    private val supabase: SupabaseClient,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(dateFormatter))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _viewMonth = MutableStateFlow(YearMonth.now().format(monthFormatter))
    val viewMonth: StateFlow<String> = _viewMonth.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    /**
     * Months that were mutated on the Today screen and whose cached report is now stale.
     * ReportScreen observes this and invalidates its own cache when the viewed month is dirty.
     */
    private val _dirtyReportMonths = MutableStateFlow<Set<String>>(emptySet())
    val dirtyReportMonths: StateFlow<Set<String>> = _dirtyReportMonths.asStateFlow()

    /**
     * Individual dates mutated on DayDetailScreen so TodayScreen can invalidate its cache.
     */
    private val _dirtyDays = MutableStateFlow<Set<String>>(emptySet())
    val dirtyDays: StateFlow<Set<String>> = _dirtyDays.asStateFlow()

    val currentUserId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    init {
        loadCategories()
    }

    fun selectDate(date: String) {
        AppLog.d(AppLog.Feature.DASHBOARD, "selectDate", "date=$date")
        _selectedDate.value = date
        val month = date.substring(0, 7)
        if (_viewMonth.value != month) {
            _viewMonth.value = month
        }
    }

    fun selectMonth(yearMonth: String) {
        _viewMonth.value = yearMonth
    }

    fun markReportMonthDirty(yearMonth: String) {
        _dirtyReportMonths.update { it + yearMonth }
    }

    fun clearDirtyReportMonth(yearMonth: String) {
        _dirtyReportMonths.update { it - yearMonth }
    }

    fun markDayDirty(date: String) {
        _dirtyDays.update { it + date }
    }

    fun clearDirtyDay(date: String) {
        _dirtyDays.update { it - date }
    }

    private fun loadCategories() {
        val userId = currentUserId ?: return
        AppLog.d(AppLog.Feature.DASHBOARD, "loadCategories", "userId=${userId.take(8)}")
        viewModelScope.launch {
            categoryRepository.getCategories(userId)
                .onSuccess { _categories.value = it }
        }
    }

    fun refreshCategories() {
        loadCategories()
    }

    fun createCategory(name: String, icon: String, color: String, type: TransactionType) {
        val userId = currentUserId ?: return
        AppLog.d(AppLog.Feature.DASHBOARD, "createCategory", "name=$name, type=$type")
        viewModelScope.launch {
            categoryRepository.createCategory(userId, name, icon, color, type, "vi")
                .onSuccess { loadCategories() }
        }
    }
}
