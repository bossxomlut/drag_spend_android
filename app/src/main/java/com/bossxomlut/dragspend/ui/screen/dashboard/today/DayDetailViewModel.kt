package com.bossxomlut.dragspend.ui.screen.dashboard.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bossxomlut.dragspend.domain.model.Transaction
import com.bossxomlut.dragspend.domain.repository.CreateTransactionRequest
import com.bossxomlut.dragspend.domain.repository.UpdateTransactionRequest
import com.bossxomlut.dragspend.domain.usecase.transaction.CreateTransactionUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.DeleteTransactionUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.GetDailyTransactionsUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.UpdateTransactionUseCase
import com.bossxomlut.dragspend.util.toFriendlyMessage
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
    private val getDailyTransactionsUseCase: GetDailyTransactionsUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayDetailUiState())
    val uiState: StateFlow<DayDetailUiState> = _uiState.asStateFlow()

    fun loadTransactions(date: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            getDailyTransactionsUseCase(date)
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
            createTransactionUseCase(request)
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
            updateTransactionUseCase(transactionId, request)
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
            deleteTransactionUseCase(transactionId)
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
