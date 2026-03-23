package com.bossxomlut.dragspend.ui.screen.dashboard.today

import android.os.Build
import androidx.annotation.RequiresApi
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
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.UserIdProvider
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
    /** Day totals for all days in the currently viewed month. Keyed by "yyyy-MM-dd". */
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
    private val userIdProvider: UserIdProvider,
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val currentUserId get() = userIdProvider.getCurrentUserId()

    /** Holds the latest debounced loadData job so it can be cancelled on rapid date changes. */
    private var loadDataJob: Job? = null

    /**
     * In-memory cache: yearMonth ("yyyy-MM") → map of date ("yyyy-MM-dd") → DayTotal.
     * Persists across date navigation within the same session.
     */
    private val monthTotalsCache = mutableMapOf<String, Map<String, DayTotal>>()

    /**
     * Per-day transaction cache: date ("yyyy-MM-dd") → list of transactions.
     * Avoids a network round-trip when navigating back to a previously loaded date.
     */
    private val dailyTransactionsCache = mutableMapOf<String, List<Transaction>>()

    /** True once cards have been successfully loaded at least once in this session. */
    private var cardsInitialized = false

    fun loadData(date: String) {
        val userId = currentUserId ?: run {
            AppLog.w(AppLog.Feature.TRANSACTION, "loadData", "no authenticated user")
            return
        }
        AppLog.d(AppLog.Feature.TRANSACTION, "loadData", "date=$date")

        // Cards are session-level data; load once, then rely on CRUD methods to keep fresh.
        if (!cardsInitialized) {
            viewModelScope.launch { loadCards(userId) }
        }

        // Cache hit: show transactions immediately with no network call.
        val cached = dailyTransactionsCache[date]
        if (cached != null) {
            AppLog.d(AppLog.Feature.TRANSACTION, "loadData", "cache hit for $date")
            _uiState.update { it.copy(transactions = cached, isLoadingTransactions = false) }
            return
        }

        // Cache miss: debounced fetch.
        loadDataJob?.cancel()
        _uiState.update { it.copy(isLoadingTransactions = true) }
        loadDataJob = viewModelScope.launch {
            delay(300L) // debounce: only fires if no new call arrives within 300 ms
            AppLog.d(AppLog.Feature.TRANSACTION, "loadData", "date=$date, executing after debounce")
            loadTransactions(userId, date)
        }
    }

    /**
     * Loads day totals for every day in the given [yearMonth] ("yyyy-MM").
     * Returns immediately from cache if already loaded; otherwise fetches and caches.
     */
    fun loadMonthlyTotals(yearMonth: String) {
        val cached = monthTotalsCache[yearMonth]
        if (cached != null) {
            AppLog.d(AppLog.Feature.TRANSACTION, "loadMonthlyTotals", "cache hit for $yearMonth")
            _uiState.update { it.copy(monthDayTotals = cached) }
            return
        }
        val userId = currentUserId ?: return
        AppLog.d(AppLog.Feature.TRANSACTION, "loadMonthlyTotals", "fetching $yearMonth")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMonthlyTotals = true) }
            transactionRepository.getMonthlyTransactions(userId, yearMonth)
                .onSuccess { txns ->
                    val totalsMap = txns
                        .groupBy { it.date }
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

    /**
     * Invalidates the cache for [yearMonth] and re-fetches so that after a transaction
     * is added/deleted the calendar grid reflects the new total.
     */
    fun invalidateMonthCache(yearMonth: String) {
        monthTotalsCache.remove(yearMonth)
        loadMonthlyTotals(yearMonth)
    }

    /**
     * Removes [date] from the daily transactions cache so the next [loadData] call re-fetches.
     */
    fun invalidateDayCache(date: String) {
        dailyTransactionsCache.remove(date)
        AppLog.d(AppLog.Feature.TRANSACTION, "invalidateDayCache", "date=$date")
    }

    private fun loadCards(userId: String) {
        AppLog.d(AppLog.Feature.CARD, "loadCards", "userId=${userId.take(8)}")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCards = true) }
            cardRepository.getCards(userId)
                .onSuccess { cards ->
                    cardsInitialized = true
                    _uiState.update { it.copy(cards = cards, isLoadingCards = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingCards = false, errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun loadTransactions(userId: String, date: String) {
        AppLog.d(AppLog.Feature.TRANSACTION, "loadTransactions", "userId=${userId.take(8)}, date=$date")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTransactions = true) }
            transactionRepository.getTransactions(userId, date)
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
        val userId = currentUserId ?: return
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
            transactionRepository.createTransaction(request)
                .onSuccess { created ->
                    // Optimistically append the new transaction (use category from card to avoid extra reload)
                    val newTx = created.copy(category = card.category)
                    val updatedList = _uiState.value.transactions + newTx
                    dailyTransactionsCache[date] = updatedList
                    _uiState.update { it.copy(transactions = updatedList) }
                    cardRepository.incrementUseCount(card.id)
                    // Invalidate calendar cache so the day dot reflects new total
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
            transactionRepository.deleteTransaction(transactionId)
                .onSuccess {
                    deletedDate?.let { date -> invalidateMonthCache(date.substring(0, 7)) }
                }
                .onFailure { e ->
                    val userId = currentUserId ?: return@launch
                    loadTransactions(userId, _uiState.value.transactions.firstOrNull()?.date ?: return@launch)
                    _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) }
                }
        }
    }

    fun updateTransaction(transactionId: String, request: UpdateTransactionRequest) {
        val userId = currentUserId ?: return
        AppLog.d(AppLog.Feature.TRANSACTION, "updateTransaction", "id=$transactionId, amount=${request.amount}, type=${request.type}")
        val oldDate = _uiState.value.transactions.firstOrNull { it.id == transactionId }?.date
        viewModelScope.launch {
            transactionRepository.updateTransaction(transactionId, request)
                .onSuccess { updated ->
                    val newDate = updated.date
                    if (oldDate != null && oldDate != newDate) {
                        // Date changed: remove from old date's cache and invalidate the new date's cache.
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
                    loadTransactions(userId, date)
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun copyFromYesterday(date: String) {
        val userId = currentUserId ?: return
        val yesterday = LocalDate.parse(date, dateFormatter).minusDays(1).format(dateFormatter)
        AppLog.d(AppLog.Feature.TRANSACTION, "copyFromYesterday", "from=$yesterday, to=$date")
        viewModelScope.launch {
            transactionRepository.copyFromYesterday(userId, yesterday, date)
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
        val userId = currentUserId ?: return
        AppLog.d(AppLog.Feature.CARD, "createCard", "title=${request.title}, type=${request.type}")
        viewModelScope.launch {
            cardRepository.createCard(request)
                .onSuccess { loadCards(userId) }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) } }
        }
    }

    fun updateCard(cardId: String, request: CreateCardRequest) {
        val userId = currentUserId ?: return
        AppLog.d(AppLog.Feature.CARD, "updateCard", "id=$cardId, title=${request.title}")
        viewModelScope.launch {
            cardRepository.updateCard(cardId, request)
                .onSuccess { loadCards(userId) }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.toFriendlyMessage()) } }
        }
    }

    fun deleteCard(cardId: String) {
        AppLog.d(AppLog.Feature.CARD, "deleteCard", "id=$cardId")
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
