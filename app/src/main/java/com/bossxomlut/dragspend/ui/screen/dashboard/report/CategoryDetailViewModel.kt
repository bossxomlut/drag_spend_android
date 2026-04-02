package com.bossxomlut.dragspend.ui.screen.dashboard.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bossxomlut.dragspend.domain.model.Transaction
import com.bossxomlut.dragspend.domain.usecase.transaction.GetMonthlyTransactionsUseCase
import com.bossxomlut.dragspend.util.toFriendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryDetailUiState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class CategoryDetailViewModel(
    private val getMonthlyTransactionsUseCase: GetMonthlyTransactionsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryDetailUiState())
    val uiState: StateFlow<CategoryDetailUiState> = _uiState.asStateFlow()

    fun load(yearMonth: String, categoryId: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            getMonthlyTransactionsUseCase(yearMonth)
                .onSuccess { all ->
                    val filtered = if (categoryId == "other") {
                        all.filter { it.categoryId == null }
                    } else {
                        all.filter { it.categoryId == categoryId }
                    }
                    _uiState.update {
                        it.copy(
                            transactions = filtered.sortedWith(
                                compareByDescending<Transaction> { t -> t.date }
                                    .thenByDescending { t -> t.position },
                            ),
                            isLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
