package com.bossxomlut.dragspend.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bossxomlut.dragspend.domain.model.Category
import com.bossxomlut.dragspend.domain.model.TransactionType
import com.bossxomlut.dragspend.domain.repository.SessionRepository
import com.bossxomlut.dragspend.domain.usecase.category.CreateCategoryUseCase
import com.bossxomlut.dragspend.domain.usecase.category.GetCategoriesUseCase
import com.bossxomlut.dragspend.util.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class DashboardViewModel(
    private val sessionRepository: SessionRepository,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(dateFormatter))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _viewMonth = MutableStateFlow(YearMonth.now().format(monthFormatter))
    val viewMonth: StateFlow<String> = _viewMonth.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _dirtyReportMonths = MutableStateFlow<Set<String>>(emptySet())
    val dirtyReportMonths: StateFlow<Set<String>> = _dirtyReportMonths.asStateFlow()

    private val _dirtyDays = MutableStateFlow<Set<String>>(emptySet())
    val dirtyDays: StateFlow<Set<String>> = _dirtyDays.asStateFlow()

    private val _isBalanceHidden = MutableStateFlow(false)
    val isBalanceHidden: StateFlow<Boolean> = _isBalanceHidden.asStateFlow()

    fun toggleBalanceVisibility() {
        _isBalanceHidden.update { !it }
    }

    val currentUserId: String?
        get() = sessionRepository.getCurrentUserId()

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
            getCategoriesUseCase()
                .onSuccess { _categories.value = it }
        }
    }

    fun refreshCategories() {
        loadCategories()
    }

    fun createCategory(name: String, icon: String, color: String, type: TransactionType) {
        AppLog.d(AppLog.Feature.DASHBOARD, "createCategory", "name=$name, type=$type")
        viewModelScope.launch {
            createCategoryUseCase(name, icon, color, type, "vi")
                .onSuccess { loadCategories() }
        }
    }
}
