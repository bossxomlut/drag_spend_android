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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class SearchParams(
    val query: String,
    val selectedCategoryIds: Set<String>,
    val startDate: String?,
    val endDate: String?,
)

private sealed interface SearchResult {
    data object Loading : SearchResult
    data object Empty : SearchResult
    data class Success(val transactions: List<Transaction>) : SearchResult
    data class Failure(val message: String) : SearchResult
}

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
        observeSearch()
    }

    // ── Input handlers ───────────────────────────────────────────────────────
    // UI only updates state — no side effects, no triggerSearch() calls.

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
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
    }

    fun onStartDateChange(date: String?) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun onEndDateChange(date: String?) {
        _uiState.update { it.copy(endDate = date) }
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
    }

    // ── Internal: categories ─────────────────────────────────────────────────

    private fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase().onSuccess { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    // ── Reactive search pipeline ─────────────────────────────────────────────
    //
    // _uiState → map(SearchParams) → debounce(300) → distinctUntilChanged
    //   → flatMapLatest(searchFlow) → catch → collect → update _uiState
    //
    // flatMapLatest automatically cancels the previous API call when new input arrives.
    // distinctUntilChanged prevents re-triggering when only results/loading change.

    private fun observeSearch() {
        viewModelScope.launch {
            _uiState
                .map { SearchParams(it.query, it.selectedCategoryIds, it.startDate, it.endDate) }
                .debounce(300)
                .distinctUntilChanged()
                .flatMapLatest { params -> searchFlow(params) }
                .catch { e -> emit(SearchResult.Failure(e.toFriendlyMessage())) }
                .collect { result ->
                    _uiState.update { state ->
                        when (result) {
                            SearchResult.Loading -> state.copy(isLoading = true, error = null)
                            SearchResult.Empty -> state.copy(
                                results = emptyList(),
                                hasSearched = false,
                                isLoading = false,
                                error = null,
                            )
                            is SearchResult.Success -> state.copy(
                                results = result.transactions,
                                isLoading = false,
                                hasSearched = true,
                            )
                            is SearchResult.Failure -> state.copy(
                                error = result.message,
                                isLoading = false,
                                hasSearched = true,
                            )
                        }
                    }
                }
        }
    }

    private fun searchFlow(params: SearchParams): Flow<SearchResult> {
        val hasFilters = params.query.isNotBlank() ||
            params.selectedCategoryIds.isNotEmpty() ||
            params.startDate != null ||
            params.endDate != null

        if (!hasFilters) return flowOf(SearchResult.Empty)

        // When no explicit date range is set, default to last 3 months to avoid pulling
        // the entire transaction history from the server.
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        val effectiveStart = params.startDate ?: today.minusMonths(3).format(dateFormatter)
        val effectiveEnd = params.endDate ?: today.format(dateFormatter)

        return flow {
            emit(SearchResult.Loading)
            searchTransactionsUseCase(
                query = params.query,
                categoryIds = params.selectedCategoryIds,
                startDate = effectiveStart,
                endDate = effectiveEnd,
            ).fold(
                onSuccess = { emit(SearchResult.Success(it)) },
                onFailure = { emit(SearchResult.Failure(it.toFriendlyMessage())) },
            )
        }
    }
}
