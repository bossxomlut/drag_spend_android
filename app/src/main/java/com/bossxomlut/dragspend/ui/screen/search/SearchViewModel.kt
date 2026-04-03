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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


// ── Models ───────────────────────────────────────────────────────────────────

data class DateRange(
    val startDate: String? = null,
    val endDate: String? = null,
)

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

private data class SearchResultState(
    val results: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false,
)

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

    // ── Independent input flows ──────────────────────────────────────────────
    // Each flow owns one dimension of user input. Updates are immediate so the
    // UI stays responsive (e.g. text field reflects every keystroke).

    private val _queryFlow = MutableStateFlow("")
    private val _categoryFlow = MutableStateFlow<Set<String>>(emptySet())
    private val _dateRangeFlow = MutableStateFlow(DateRange())
    private val _categoriesFlow = MutableStateFlow<List<Category>>(emptyList())

    // ── Reactive search pipeline ─────────────────────────────────────────────
    //
    // queryFlow ── debounce(300) ── distinctUntilChanged ──┐
    // categoryFlow (StateFlow, already distinct) ──────────┤ combine → SearchParams
    // dateRangeFlow (StateFlow, already distinct) ─────────┘        │
    //                                                     distinctUntilChanged
    //                                                              │
    //                                          flatMapLatest → searchFlow(params)
    //                                                              │
    //                                                    catch → Failure
    //                                                              │
    //                                                  collect → _searchResultState
    //
    // flatMapLatest automatically cancels the previous API call when new params arrive.
    // Debounce only applies to the query flow — category and date changes trigger
    // immediately since StateFlow already guarantees distinct emissions.

    private val searchParamsFlow: Flow<SearchParams> = combine(
        _queryFlow.debounce(300).distinctUntilChanged(),
        _categoryFlow,
        _dateRangeFlow,
    ) { query, categoryIds, dateRange ->
        SearchParams(query, categoryIds, dateRange.startDate, dateRange.endDate)
    }.distinctUntilChanged()

    private val _searchResultState = MutableStateFlow(SearchResultState())

    // ── Public UI state ──────────────────────────────────────────────────────
    // Derived from combining all input flows with the search result accumulator.
    // stateIn makes this a hot StateFlow scoped to viewModelScope.

    val uiState: StateFlow<SearchUiState> = combine(
        _queryFlow,
        _categoryFlow,
        _dateRangeFlow,
        _categoriesFlow,
        _searchResultState,
    ) { query, categoryIds, dateRange, categories, searchResult ->
        SearchUiState(
            query = query,
            selectedCategoryIds = categoryIds,
            startDate = dateRange.startDate,
            endDate = dateRange.endDate,
            categories = categories,
            isLoading = searchResult.isLoading,
            results = searchResult.results,
            error = searchResult.error,
            hasSearched = searchResult.hasSearched,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState(),
    )

    init {
        loadCategories()
        observeSearchParams()
    }

    // ── Input handlers ───────────────────────────────────────────────────────
    // Each handler updates only its own flow — no cross-contamination.

    fun onQueryChange(query: String) {
        _queryFlow.value = query
    }

    fun toggleCategory(categoryId: String) {
        _categoryFlow.update { ids ->
            if (categoryId in ids) ids - categoryId else ids + categoryId
        }
    }

    fun onStartDateChange(date: String?) {
        _dateRangeFlow.update { it.copy(startDate = date) }
    }

    fun onEndDateChange(date: String?) {
        _dateRangeFlow.update { it.copy(endDate = date) }
    }

    fun clearFilters() {
        _queryFlow.value = ""
        _categoryFlow.value = emptySet()
        _dateRangeFlow.value = DateRange()
        _searchResultState.value = SearchResultState()
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase().onSuccess { cats ->
                _categoriesFlow.value = cats
            }
        }
    }

    private fun observeSearchParams() {
        viewModelScope.launch {
            searchParamsFlow
                .flatMapLatest { params -> searchFlow(params) }
                .catch { e -> emit(SearchResult.Failure(e.toFriendlyMessage())) }
                .collect(::applySearchResult)
        }
    }

    private fun applySearchResult(result: SearchResult) {
        _searchResultState.update { prev -> result.toResultState(prev) }
    }

    private fun SearchResult.toResultState(prev: SearchResultState): SearchResultState = when (this) {
        SearchResult.Loading -> prev.copy(isLoading = true, error = null)
        SearchResult.Empty -> SearchResultState()
        is SearchResult.Success -> SearchResultState(results = transactions, hasSearched = true)
        is SearchResult.Failure -> SearchResultState(error = message, hasSearched = true)
    }

    private fun searchFlow(params: SearchParams): Flow<SearchResult> {
        val hasFilters = params.query.isNotBlank() ||
            params.selectedCategoryIds.isNotEmpty() ||
            params.startDate != null ||
            params.endDate != null

        if (!hasFilters) return flowOf(SearchResult.Empty)

        // When no explicit date range is set, default to last 3 months to avoid pulling
        // the entire transaction history from the server.
        val today = LocalDate.now()
        val effectiveStart = params.startDate ?: today.minusMonths(3).format(DATE_FORMATTER)
        val effectiveEnd = params.endDate ?: today.format(DATE_FORMATTER)

        return flow {
            searchTransactionsUseCase(
                query = params.query,
                categoryIds = params.selectedCategoryIds,
                startDate = effectiveStart,
                endDate = effectiveEnd,
            ).fold(
                onSuccess = { emit(SearchResult.Success(it)) },
                onFailure = { emit(SearchResult.Failure(it.toFriendlyMessage())) },
            )
        }.onStart { emit(SearchResult.Loading) }
    }

    private companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}
