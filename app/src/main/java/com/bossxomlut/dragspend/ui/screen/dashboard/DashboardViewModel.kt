package com.bossxomlut.dragspend.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bossxomlut.dragspend.data.model.Category
import com.bossxomlut.dragspend.domain.repository.CategoryRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val currentUserId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    init {
        loadCategories()
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        val month = date.substring(0, 7)
        if (_viewMonth.value != month) {
            _viewMonth.value = month
        }
    }

    fun selectMonth(yearMonth: String) {
        _viewMonth.value = yearMonth
    }

    private fun loadCategories() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            categoryRepository.getCategories(userId)
                .onSuccess { _categories.value = it }
        }
    }

    fun refreshCategories() {
        loadCategories()
    }
}
