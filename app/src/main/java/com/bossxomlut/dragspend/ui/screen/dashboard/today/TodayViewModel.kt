package com.bossxomlut.dragspend.ui.screen.dashboard.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bossxomlut.dragspend.data.model.DayTotal
import com.bossxomlut.dragspend.data.model.SpendingCard
import com.bossxomlut.dragspend.data.model.Transaction
import com.bossxomlut.dragspend.data.model.TransactionType
import com.bossxomlut.dragspend.domain.repository.CardRepository
import com.bossxomlut.dragspend.domain.repository.CreateCardRequest
import com.bossxomlut.dragspend.domain.repository.CreateTransactionRequest
import com.bossxomlut.dragspend.domain.repository.CreateVariantRequest
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class TodayUiState(
    val cards: List<SpendingCard> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val isLoadingCards: Boolean = false,
    val isLoadingTransactions: Boolean = false,
    val errorMessage: String? = null,
) {
    val dayTotal: DayTotal
        get() {
            val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            return DayTotal(date = "", income = income, expense = expense)
        }
}


class TodayViewModel(
    private val supabase: SupabaseClient,
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val currentUserId get() = supabase.auth.currentUserOrNull()?.id

    fun loadData(date: String) {
        val userId = currentUserId ?: return
        loadCards(userId)
        loadTransactions(userId, date)
    }

    private fun loadCards(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCards = true) }
            cardRepository.getCards(userId)
                .onSuccess { cards ->
                    _uiState.update { it.copy(cards = cards, isLoadingCards = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingCards = false, errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun loadTransactions(userId: String, date: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTransactions = true) }
            transactionRepository.getTransactions(userId, date)
                .onSuccess { txns ->
                    _uiState.update { it.copy(transactions = txns, isLoadingTransactions = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingTransactions = false, errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun addTransactionFromCard(card: SpendingCard, date: String) {
        val userId = currentUserId ?: return
        val amount = card.defaultAmount
        if (amount <= 0L) return

        viewModelScope.launch {
            val request = CreateTransactionRequest(
                userId = userId,
                sourceCardId = card.id,
                date = date,
                title = card.title,
                amount = amount,
                categoryId = card.categoryId,
                type = card.type,
                note = null,
            )
            transactionRepository.createTransaction(request)
                .onSuccess {
                    loadTransactions(userId, date)
                    cardRepository.incrementUseCount(card.id)
                    loadCards(userId)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun deleteTransaction(transactionId: String) {
        _uiState.update { it.copy(transactions = it.transactions.filterNot { t -> t.id == transactionId }) }
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transactionId)
                .onFailure { e ->
                    val userId = currentUserId ?: return@launch
                    loadTransactions(userId, _uiState.value.transactions.firstOrNull()?.date ?: return@launch)
                    _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun updateTransaction(transactionId: String, request: UpdateTransactionRequest) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            transactionRepository.updateTransaction(transactionId, request)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(transactions = state.transactions.map { t ->
                            if (t.id == transactionId) updated else t
                        })
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) }
                    val date = _uiState.value.transactions.firstOrNull()?.date ?: return@launch
                    loadTransactions(userId, date)
                }
        }
    }

    fun copyFromYesterday(date: String) {
        val userId = currentUserId ?: return
        val yesterday = LocalDate.parse(date, dateFormatter).minusDays(1).format(dateFormatter)
        viewModelScope.launch {
            transactionRepository.copyFromYesterday(userId, yesterday, date)
                .onSuccess { copied ->
                    _uiState.update { it.copy(transactions = it.transactions + copied) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun createCard(request: CreateCardRequest) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            cardRepository.createCard(request)
                .onSuccess { loadCards(userId) }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) } }
        }
    }

    fun updateCard(cardId: String, request: CreateCardRequest) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            cardRepository.updateCard(cardId, request)
                .onSuccess { loadCards(userId) }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) } }
        }
    }

    fun deleteCard(cardId: String) {
        viewModelScope.launch {
            cardRepository.deleteCard(cardId)
                .onSuccess { _uiState.update { it.copy(cards = it.cards.filterNot { c -> c.id == cardId }) } }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) } }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
