package com.bossxomlut.dragspend.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bossxomlut.dragspend.domain.model.Category
import com.bossxomlut.dragspend.domain.model.Transaction
import com.bossxomlut.dragspend.domain.usecase.category.GetCategoriesUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.SearchTransactionsUseCase
import com.bossxomlut.dragspend.util.toFriendlyMessage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedCategoryIds: Set<String> = emptySet(),
    val startDate: String? = null,
    val endDate: String? = null,
    val results: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false,
)

class SearchViewModel(
    private val searchTransactionsUseCase: SearchTransactionsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase().onSuccess { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        triggerSearch()
    }

    fun toggleCategory(categoryId: String) {
        _uiState.update { state ->
            val updated = if (categoryId in state.selectedCategoryIds) {
                state.selectedCategoryIds - categoryId
            } else {
                state.selectedCategoryIds + categoryId
            }
            state.copy(selectedCategoryIds = updated)
        }
        triggerSearch()
    }

    fun onStartDateChange(date: String?) {
        _uiState.update { it.copy(startDate = date) }
        triggerSearch()
    }

    fun onEndDateChange(date: String?) {
        _uiState.update { it.copy(endDate = date) }
        triggerSearch()
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                query = "",
                selectedCategoryIds = emptySet(),
                startDate = null,
                endDate = null,
                results = emptyList(),
                hasSearched = false,
                error = null,
            )
        }
        searchJob?.cancel()
    }

    private var searchJob: Job? = null

    private fun triggerSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            performSearch()
        }
    }

    private suspend fun performSearch() {
        val state = _uiState.value
        val hasFilters = state.query.isNotBlank() ||
            state.selectedCategoryIds.isNotEmpty() ||
            state.startDate != null ||
            state.endDate != null

        if (!hasFilters) {
            _uiState.update { it.copy(results = emptyList(), hasSearched = false, isLoading = false) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        // When no explicit date range is set, default to last 3 months to avoid pulling
        // the entire transaction history from the server.
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        val effectiveStart = state.startDate ?: today.minusMonths(3).format(dateFormatter)
        val effectiveEnd = state.endDate ?: today.format(dateFormatter)

        searchTransactionsUseCase(
            query = state.query,
            categoryIds = state.selectedCategoryIds,
            startDate = effectiveStart,
            endDate = effectiveEnd,
        ).onSuccess { transactions ->
            _uiState.update { it.copy(results = transactions, isLoading = false, hasSearched = true) }
        }.onFailure { e ->
            _uiState.update { it.copy(error = e.message, isLoading = false, hasSearched = true) }
        }
    }
}
