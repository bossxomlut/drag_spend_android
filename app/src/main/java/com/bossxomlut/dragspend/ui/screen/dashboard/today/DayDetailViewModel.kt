package com.bossxomlut.dragspend.ui.screen.dashboard.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bossxomlut.dragspend.data.model.Transaction
import com.bossxomlut.dragspend.domain.repository.CreateTransactionRequest
import com.bossxomlut.dragspend.domain.repository.TransactionRepository
import com.bossxomlut.dragspend.domain.repository.UpdateTransactionRequest
import com.bossxomlut.dragspend.util.toFriendlyMessage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DayDetailUiState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class DayDetailViewModel(
    private val supabase: SupabaseClient,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayDetailUiState())
    val uiState: StateFlow<DayDetailUiState> = _uiState.asStateFlow()

    private val currentUserId get() = supabase.auth.currentUserOrNull()?.id

    fun loadTransactions(date: String) {
        val userId = currentUserId ?: return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            transactionRepository.getTransactions(userId, date)
                .onSuccess { txns ->
                    _uiState.update { it.copy(transactions = txns, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun addTransaction(request: CreateTransactionRequest) {
        viewModelScope.launch {
            transactionRepository.createTransaction(request)
                .onSuccess { created ->
                    _uiState.update { state ->
                        state.copy(transactions = state.transactions + created)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun updateTransaction(transactionId: String, request: UpdateTransactionRequest) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transactionId, request)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            transactions = state.transactions.map { t ->
                                if (t.id == transactionId) updated else t
                            },
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun deleteTransaction(transactionId: String) {
        _uiState.update { state ->
            state.copy(transactions = state.transactions.filterNot { it.id == transactionId })
        }
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transactionId)
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
