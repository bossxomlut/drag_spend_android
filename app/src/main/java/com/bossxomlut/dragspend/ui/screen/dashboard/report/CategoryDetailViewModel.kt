package com.bossxomlut.dragspend.ui.screen.dashboard.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bossxomlut.dragspend.data.model.Transaction
import com.bossxomlut.dragspend.domain.repository.TransactionRepository
import com.bossxomlut.dragspend.util.toFriendlyMessage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
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
    private val supabase: SupabaseClient,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryDetailUiState())
    val uiState: StateFlow<CategoryDetailUiState> = _uiState.asStateFlow()

    private val currentUserId get() = supabase.auth.currentUserOrNull()?.id

    /**
     * Loads all transactions for [yearMonth] and filters them by [categoryId].
     * Pass [categoryId] = "other" to see uncategorised transactions.
     */
    fun load(yearMonth: String, categoryId: String) {
        val userId = currentUserId ?: return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            transactionRepository.getMonthlyTransactions(userId, yearMonth)
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
