package com.bossxomlut.dragspend.ui.screen.dashboard.today

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bossxomlut.dragspend.domain.model.DayTotal
import com.bossxomlut.dragspend.domain.model.SpendingCard
import com.bossxomlut.dragspend.domain.model.Transaction
import com.bossxomlut.dragspend.domain.model.TransactionType
import com.bossxomlut.dragspend.domain.repository.CreateCardRequest
import com.bossxomlut.dragspend.domain.repository.CreateTransactionRequest
import com.bossxomlut.dragspend.domain.repository.SessionRepository
import com.bossxomlut.dragspend.domain.repository.UpdateTransactionRequest
import com.bossxomlut.dragspend.domain.usecase.card.CreateCardUseCase
import com.bossxomlut.dragspend.domain.usecase.card.DeleteCardUseCase
import com.bossxomlut.dragspend.domain.usecase.card.GetCardsUseCase
import com.bossxomlut.dragspend.domain.usecase.card.IncrementCardUseCountUseCase
import com.bossxomlut.dragspend.domain.usecase.card.UpdateCardUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.CopyFromYesterdayUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.CreateTransactionUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.DeleteTransactionUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.GetDailyTransactionsUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.GetMonthlyTransactionsUseCase
import com.bossxomlut.dragspend.domain.usecase.transaction.UpdateTransactionUseCase
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.toFriendlyMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val isLoadingMonthlyTotals: Boolean = false,
    val monthDayTotals: Map<String, DayTotal> = emptyMap(),
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
    private val sessionRepository: SessionRepository,
    private val getCardsUseCase: GetCardsUseCase,
    private val createCardUseCase: CreateCardUseCase,
    private val updateCardUseCase: UpdateCardUseCase,
    private val deleteCardUseCase: DeleteCardUseCase,
    private val incrementCardUseCountUseCase: IncrementCardUseCountUseCase,
    private val getDailyTransactionsUseCase: GetDailyTransactionsUseCase,
    private val getMonthlyTransactionsUseCase: GetMonthlyTransactionsUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val copyFromYesterdayUseCase: CopyFromYesterdayUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private var loadDataJob: Job? = null
    private val monthTotalsCache = mutableMapOf<String, Map<String, DayTotal>>()
    private val dailyTransactionsCache = mutableMapOf<String, List<Transaction>>()
    private var cardsInitialized = false

    fun loadData(date: String) {
        AppLog.d(AppLog.Feature.TRANSACTION, "loadData", "date=$date")

        if (!cardsInitialized) {
            viewModelScope.launch { loadCards() }
        }

        val cached = dailyTransactionsCache[date]
        if (cached != null) {
            AppLog.d(AppLog.Feature.TRANSACTION, "loadData", "cache hit for $date")
            _uiState.update { it.copy(transactions = cached, isLoadingTransactions = false) }
            return
        }

        loadDataJob?.cancel()
        _uiState.update { it.copy(isLoadingTransactions = true) }
        loadDataJob = viewModelScope.launch {
            delay(300L)
            AppLog.d(AppLog.Feature.TRANSACTION, "loadData", "date=$date, executing after debounce")
            loadTransactions(date)
        }
    }

    fun loadMonthlyTotals(yearMonth: String) {
        val cached = monthTotalsCache[yearMonth]
        if (cached != null) {
            AppLog.d(AppLog.Feature.TRANSACTION, "loadMonthlyTotals", "cache hit for $yearMonth")
            _uiState.update { it.copy(monthDayTotals = cached) }
            return
        }
        AppLog.d(AppLog.Feature.TRANSACTION, "loadMonthlyTotals", "fetching $yearMonth")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMonthlyTotals = true) }
            getMonthlyTransactionsUseCase(yearMonth)
                .onSuccess { txns ->
                    val totalsMap = txns.groupBy { it.date }
                        .mapValues { (date, list) ->
                            val inc = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                            val exp = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                            DayTotal(date = date, income = inc, expense = exp)
                        }
                    monthTotalsCache[yearMonth] = totalsMap
                    _uiState.update { it.copy(monthDayTotals = totalsMap, isLoadingMonthlyTotals = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingMonthlyTotals = false, errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun invalidateMonthCache(yearMonth: String) {
        monthTotalsCache.remove(yearMonth)
        loadMonthlyTotals(yearMonth)
    }

    fun invalidateDayCache(date: String) {
        dailyTransactionsCache.remove(date)
        AppLog.d(AppLog.Feature.TRANSACTION, "invalidateDayCache", "date=$date")
    }

    private fun loadCards() {
        AppLog.d(AppLog.Feature.CARD, "loadCards")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCards = true) }
            getCardsUseCase()
                .onSuccess { cards ->
                    cardsInitialized = true
                    _uiState.update { it.copy(cards = cards, isLoadingCards = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingCards = false, errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun loadTransactions(date: String) {
        AppLog.d(AppLog.Feature.TRANSACTION, "loadTransactions", "date=$date")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTransactions = true) }
            getDailyTransactionsUseCase(date)
                .onSuccess { txns ->
                    dailyTransactionsCache[date] = txns
                    _uiState.update { it.copy(transactions = txns, isLoadingTransactions = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingTransactions = false, errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun addTransactionFromCard(card: SpendingCard, date: String, amount: Long = card.defaultAmount) {
        val userId = sessionRepository.getLocalUserId()
        AppLog.d(AppLog.Feature.TRANSACTION, "addTransactionFromCard", "cardId=${card.id}, title=${card.title}, amount=$amount, date=$date")
        if (amount <= 0L) {
            _uiState.update {
                it.copy(errorMessage = "Card \"${card.title}\" has no amount set. Edit the card to add a price option.")
            }
            return
        }

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
            createTransactionUseCase(request)
                .onSuccess { created ->
                    val newTx = created.copy(category = card.category)
                    val updatedList = _uiState.value.transactions + newTx
                    dailyTransactionsCache[date] = updatedList
                    _uiState.update { it.copy(transactions = updatedList) }
                    incrementCardUseCountUseCase(card.id)
                    invalidateMonthCache(date.substring(0, 7))
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun deleteTransaction(transactionId: String) {
        AppLog.d(AppLog.Feature.TRANSACTION, "deleteTransaction", "id=$transactionId")
        val deletedDate = _uiState.value.transactions.firstOrNull { it.id == transactionId }?.date
        val updatedTransactions = _uiState.value.transactions.filterNot { t -> t.id == transactionId }
        deletedDate?.let { dailyTransactionsCache[it] = updatedTransactions }
        _uiState.update { it.copy(transactions = updatedTransactions) }
        viewModelScope.launch {
            deleteTransactionUseCase(transactionId)
                .onSuccess {
                    deletedDate?.let { date -> invalidateMonthCache(date.substring(0, 7)) }
                }
                .onFailure { e ->
                    val date = _uiState.value.transactions.firstOrNull()?.date ?: return@launch
                    loadTransactions(date)
                    _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun updateTransaction(transactionId: String, request: UpdateTransactionRequest) {
        AppLog.d(AppLog.Feature.TRANSACTION, "updateTransaction", "id=$transactionId, amount=${request.amount}, type=${request.type}")
        val oldDate = _uiState.value.transactions.firstOrNull { it.id == transactionId }?.date
        viewModelScope.launch {
            updateTransactionUseCase(transactionId, request)
                .onSuccess { updated ->
                    val newDate = updated.date
                    if (oldDate != null && oldDate != newDate) {
                        dailyTransactionsCache[oldDate] = dailyTransactionsCache[oldDate]
                            ?.filterNot { it.id == transactionId } ?: emptyList()
                        dailyTransactionsCache.remove(newDate)
                    } else {
                        val updatedList = _uiState.value.transactions.map { t ->
                            if (t.id == transactionId) updated else t
                        }
                        dailyTransactionsCache[newDate] = updatedList
                    }
                    _uiState.update { state ->
                        state.copy(transactions = state.transactions.map { t ->
                            if (t.id == transactionId) updated else t
                        })
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) }
                    val date = _uiState.value.transactions.firstOrNull()?.date ?: return@launch
                    loadTransactions(date)
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun copyFromYesterday(date: String) {
        val yesterday = LocalDate.parse(date, dateFormatter).minusDays(1).format(dateFormatter)
        AppLog.d(AppLog.Feature.TRANSACTION, "copyFromYesterday", "from=$yesterday, to=$date")
        viewModelScope.launch {
            copyFromYesterdayUseCase(yesterday, date)
                .onSuccess { copied ->
                    val updatedList = _uiState.value.transactions + copied
                    dailyTransactionsCache[date] = updatedList
                    _uiState.update { it.copy(transactions = updatedList) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun createCard(request: CreateCardRequest) {
        AppLog.d(AppLog.Feature.CARD, "createCard", "title=${request.title}, type=${request.type}")
        viewModelScope.launch {
            createCardUseCase(request)
                .onSuccess { loadCards() }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) } }
        }
    }

    fun updateCard(cardId: String, request: CreateCardRequest) {
        AppLog.d(AppLog.Feature.CARD, "updateCard", "id=$cardId, title=${request.title}")
        viewModelScope.launch {
            updateCardUseCase(cardId, request)
                .onSuccess { loadCards() }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) } }
        }
    }

    fun deleteCard(cardId: String) {
        AppLog.d(AppLog.Feature.CARD, "deleteCard", "id=$cardId")
        viewModelScope.launch {
            deleteCardUseCase(cardId)
                .onSuccess { _uiState.update { it.copy(cards = it.cards.filterNot { c -> c.id == cardId }) } }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) } }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
