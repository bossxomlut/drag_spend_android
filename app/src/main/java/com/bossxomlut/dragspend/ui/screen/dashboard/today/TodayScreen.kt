package com.bossxomlut.dragspend.ui.screen.dashboard.today

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import com.bossxomlut.dragspend.R
import com.bossxomlut.dragspend.data.model.SpendingCard
import com.bossxomlut.dragspend.data.model.Transaction
import com.bossxomlut.dragspend.data.model.TransactionType
import com.bossxomlut.dragspend.ui.components.AppToast
import com.bossxomlut.dragspend.ui.components.CategoryIcon
import com.bossxomlut.dragspend.ui.components.ConfirmDialog
import com.bossxomlut.dragspend.ui.components.ToastType
import com.bossxomlut.dragspend.ui.screen.dashboard.DashboardViewModel
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme
import com.bossxomlut.dragspend.util.CurrencyFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Root screen
// ---------------------------------------------------------------------------

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    dashboardViewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    todayViewModel: TodayViewModel = koinViewModel(),
) {
    val uiState by todayViewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by dashboardViewModel.selectedDate.collectAsStateWithLifecycle()
    val categories by dashboardViewModel.categories.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val userId = dashboardViewModel.currentUserId ?: ""
    val language = "vi"
    var addedCardIds by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(selectedDate) {
        todayViewModel.loadData(selectedDate)
        addedCardIds = setOf()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            toastMessage = it
            todayViewModel.clearError()
        }
    }

    var showCreateCard by remember { mutableStateOf(false) }
    var editCard by remember { mutableStateOf<SpendingCard?>(null) }
    var editTransaction by remember { mutableStateOf<Transaction?>(null) }
    var deleteCardId by remember { mutableStateOf<String?>(null) }
    var deleteTransactionId by remember { mutableStateOf<String?>(null) }
    var deleteTransactionSourceCardId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCards by remember {
        derivedStateOf {
            val base = if (searchQuery.isBlank()) uiState.cards
            else uiState.cards.filter { it.title.contains(searchQuery, ignoreCase = true) }
            base.filter { it.id !in addedCardIds }
        }
    }

    // Drag state
    var draggingCard by remember { mutableStateOf<SpendingCard?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var draggingAmount by remember { mutableStateOf(0L) }
    var boxRootOrigin by remember { mutableStateOf(Offset.Zero) }
    var dayViewBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    fun isInDropZone(offset: Offset): Boolean {
        val bounds = dayViewBounds ?: return false
        val dropZone = bounds.copy(bottom = bounds.top + bounds.height / 2)
        return dropZone.contains(offset)
    }

    val isDragOverDropArea by remember {
        derivedStateOf {
            if (draggingCard == null) false
            else isInDropZone(dragStartOffset + dragOffset)
        }
    }

    Box(
        modifier = modifier.onGloballyPositioned { coords ->
            boxRootOrigin = coords.boundsInRoot().topLeft
        },
    ) {
        val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
                skipHiddenState = true,
            ),
        )

        BottomSheetScaffold(
            modifier = Modifier.fillMaxSize(),
            scaffoldState = bottomSheetScaffoldState,
            sheetPeekHeight = 80.dp,
            sheetContainerColor = MaterialTheme.colorScheme.surface,
            sheetTonalElevation = 2.dp,
            sheetShadowElevation = 4.dp,
            sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    },
                    actions = {
                        // Date chip — shows selected date; tap to jump back to today
                        val appBarDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        val appBarToday = LocalDate.now().format(appBarDateFormatter)
                        val isViewingToday = selectedDate == appBarToday
                        val chipDateLabel = runCatching {
                            LocalDate.parse(selectedDate, appBarDateFormatter)
                                .format(DateTimeFormatter.ofPattern("dd/MM (EEE)", Locale.getDefault()))
                        }.getOrElse { stringResource(R.string.tab_today) }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isViewingToday) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(enabled = !isViewingToday) {
                                    dashboardViewModel.selectDate(appBarToday)
                                },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = if (isViewingToday) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = chipDateLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isViewingToday) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        // Profile icon
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = stringResource(R.string.label_profile),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
            sheetContent = {
                SpendingCardsPanel(
                    cards = filteredCards,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onCardAdd = { card, amount ->
                        todayViewModel.addTransactionFromCard(card, selectedDate, amount)
                        addedCardIds = addedCardIds + card.id
                    },
                    onCardEdit = { editCard = it },
                    onCardDelete = { deleteCardId = it.id },
                    onDragStart = { card, startOffset, amount ->
                        draggingCard = card
                        dragStartOffset = startOffset
                        draggingAmount = amount
                        dragOffset = Offset.Zero
                    },
                    onDragEnd = { card ->
                        val finalOffset = dragStartOffset + dragOffset
                        if (isInDropZone(finalOffset)) {
                            todayViewModel.addTransactionFromCard(card, selectedDate, draggingAmount)
                            addedCardIds = addedCardIds + card.id
                        }
                        draggingCard = null
                        dragOffset = Offset.Zero
                        dragStartOffset = Offset.Zero
                        draggingAmount = 0L
                    },
                    onDragMove = { delta -> dragOffset += delta },
                    onAddCard = { showCreateCard = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) { sheetPadding ->
            DayView(
                selectedDate = selectedDate,
                transactions = uiState.transactions,
                dayTotal = uiState.dayTotal,
                onDateChange = { dashboardViewModel.selectDate(it) },
                onEditTransaction = { editTransaction = it },
                onDeleteTransaction = {
                        deleteTransactionId = it.id
                        deleteTransactionSourceCardId = it.sourceCardId
                    },
                onCopyFromYesterday = { todayViewModel.copyFromYesterday(selectedDate) },
                isDragging = draggingCard != null,
                isDragOver = isDragOverDropArea,
                isLoading = uiState.isLoadingTransactions,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(sheetPadding)
                    .onGloballyPositioned { coords ->
                        dayViewBounds = coords.boundsInRoot()
                    },
            )
        }

        // Floating drag ghost
        if (draggingCard != null) {
            val ghostCard = draggingCard!!
            val ghostAccent = if (ghostCard.type == TransactionType.EXPENSE) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.tertiary
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 12.dp,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, ghostAccent),
                modifier = Modifier
                    .offset {
                        val pos = dragStartOffset - boxRootOrigin + dragOffset
                        IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
                    }
                    .padding(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    ghostCard.category?.let { cat ->
                        CategoryIcon(icon = cat.icon, color = cat.color, size = 20.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column {
                        Text(
                            text = ghostCard.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (draggingAmount > 0L) {
                            Text(
                                text = CurrencyFormatter.formatCompact(draggingAmount),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ghostAccent,
                            )
                        }
                    }
                }
            }
        }

        AppToast(
            message = toastMessage,
            type = ToastType.ERROR,
            onDismiss = { toastMessage = null },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp),
        )
    }

    if (showCreateCard) {
        CreateCardDialog(
            userId = userId,
            language = language,
            categories = categories,
            onSave = { req ->
                todayViewModel.createCard(req)
                showCreateCard = false
            },
            onDismiss = { showCreateCard = false },
        )
    }

    editCard?.let { card ->
        CreateCardDialog(
            userId = userId,
            language = language,
            categories = categories,
            editCard = card,
            onSave = { req ->
                todayViewModel.updateCard(card.id, req)
                editCard = null
            },
            onDismiss = { editCard = null },
        )
    }

    editTransaction?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            categories = categories,
            cards = uiState.cards,
            onSave = { req ->
                todayViewModel.updateTransaction(tx.id, req)
                editTransaction = null
            },
            onDismiss = { editTransaction = null },
        )
    }

    deleteCardId?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.delete_card_title),
            message = stringResource(R.string.delete_card_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                todayViewModel.deleteCard(id)
                deleteCardId = null
            },
            onDismiss = { deleteCardId = null },
            isDestructive = true,
        )
    }

    deleteTransactionId?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.delete_transaction_title),
            message = stringResource(R.string.delete_transaction_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                todayViewModel.deleteTransaction(id)
                deleteTransactionSourceCardId?.let { cardId ->
                    addedCardIds = addedCardIds - cardId
                }
                deleteTransactionId = null
                deleteTransactionSourceCardId = null
            },
            onDismiss = {
                deleteTransactionId = null
                deleteTransactionSourceCardId = null
            },
            isDestructive = true,
        )
    }
}

// ---------------------------------------------------------------------------
// Day view — date navigation header + transaction list
// ---------------------------------------------------------------------------

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayView(
    selectedDate: String,
    transactions: List<Transaction>,
    dayTotal: com.bossxomlut.dragspend.data.model.DayTotal,
    onDateChange: (String) -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onCopyFromYesterday: () -> Unit,
    isDragging: Boolean = false,
    isDragOver: Boolean = false,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.parse(selectedDate, dateFormatter)
    val isToday = today == LocalDate.now()
    var showDatePicker by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "dropZonePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Date navigation header ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { onDateChange(today.minusDays(1).format(dateFormatter)) },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.action_previous_day),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = today.dayOfWeek.getDisplayName(
                                TextStyle.FULL,
                                Locale.getDefault(),
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        if (isToday) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Text(
                                    text = stringResource(R.string.tab_today),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = today.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }

                IconButton(
                    onClick = { onDateChange(today.plusDays(1).format(dateFormatter)) },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.action_next_day),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Day summary row ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Expense total badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "-${CurrencyFormatter.formatCompact(dayTotal.expense)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "-${dayTotal.expense}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "· ${
                        pluralStringResource(
                            id = R.plurals.transaction_count,
                            count = transactions.size,
                            transactions.size,
                        )
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            // ── Transaction list ───────────────────────────────────────────────
            if (isLoading) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(4) {
                        ShimmerTransactionItem()
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onEdit = { onEditTransaction(transaction) },
                            onDelete = { onDeleteTransaction(transaction) },
                            modifier = Modifier.animateItem(
                                fadeInSpec = spring(stiffness = Spring.StiffnessMedium),
                                fadeOutSpec = spring(stiffness = Spring.StiffnessMedium),
                                placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            ),
                        )
                    }

                    if (transactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_transactions),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    if (transactions.isEmpty()) {
                        item {
                            TextButton(
                                onClick = onCopyFromYesterday,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.action_copy_from_yesterday))
                            }
                        }
                    }
                }
            }

            // Drop zone overlay shown while dragging
            if (isDragging) {
                val overlayColor = if (isDragOver) {
                    MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                }
                val borderColor = if (isDragOver) {
                    MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha + 0.4f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = if (isDragOver) 2.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
                        )
                        .background(overlayColor),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDragOver) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.action_add),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        } // end Box

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = today
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli(),
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selected = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onDateChange(selected.format(dateFormatter))
                        }
                        showDatePicker = false
                    }) {
                        Text(stringResource(R.string.action_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            ) {
                DatePicker(state = datePickerState)
            }
        }
    } // end DayView Box

    // ---------------------------------------------------------------------------
    // Spending cards bottom panel
    // ---------------------------------------------------------------------------
}

@Composable
private fun SpendingCardsPanel(
        cards: List<SpendingCard>,
        searchQuery: String,
        onSearchChange: (String) -> Unit,
        onCardAdd: (SpendingCard, Long) -> Unit,
        onCardEdit: (SpendingCard) -> Unit,
        onCardDelete: (SpendingCard) -> Unit,
        onDragStart: (SpendingCard, Offset, Long) -> Unit,
        onDragEnd: (SpendingCard) -> Unit,
        onDragMove: (Offset) -> Unit,
        onAddCard: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
        val displayCards = cards.filter { it.type == selectedType }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
        ) {
            // Header row: title + add button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.label_spending_cards),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FilledTonalIconButton(
                    onClick = onAddCard,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_new_card))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        stringResource(R.string.placeholder_search_cards),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Type toggle row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { type ->
                    val isSelected = selectedType == type
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedType = type },
                    ) {
                        Text(
                            text = if (type == TransactionType.EXPENSE) {
                                stringResource(R.string.type_expense)
                            } else {
                                stringResource(R.string.type_income)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Cards list
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(displayCards, key = { it.id }) { card ->
                    CompactCardChip(
                        card = card,
                        onAdd = { amount -> onCardAdd(card, amount) },
                        onEdit = { onCardEdit(card) },
                        onDelete = { onCardDelete(card) },
                        onDragStart = { startOffset, amount -> onDragStart(card, startOffset, amount) },
                        onDragEnd = { onDragEnd(card) },
                        onDragMove = onDragMove,
                        modifier = Modifier.animateItem(
                            fadeInSpec = spring(stiffness = Spring.StiffnessMedium),
                            fadeOutSpec = spring(stiffness = Spring.StiffnessMedium),
                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    // ---------------------------------------------------------------------------
    // Compact card chip — shown in the bottom cards panel
    // ---------------------------------------------------------------------------

    @Composable
    private fun CompactCardChip(
        card: SpendingCard,
        onAdd: (amount: Long) -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onDragStart: (Offset, Long) -> Unit,
        onDragEnd: () -> Unit,
        onDragMove: (Offset) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        var showMenu by remember { mutableStateOf(false) }
        var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
        var selectedVariantId by remember(card.id) { mutableStateOf(card.defaultVariant?.id) }

        val selectedVariant = card.variants.firstOrNull { it.id == selectedVariantId } ?: card.defaultVariant
        val selectedAmount = selectedVariant?.amount ?: 0L

        val accentColor = if (card.type == TransactionType.EXPENSE) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.tertiary
        }

        Box(modifier = modifier.onGloballyPositioned { layoutCoordinates = it }) {
            Card(
                modifier = Modifier
                    .width(150.dp)
                    .pointerInput(card.id) {
                        detectDragGestures(
                            onDragStart = { localOffset ->
                                val globalOffset = layoutCoordinates?.localToRoot(localOffset) ?: localOffset
                                onDragStart(globalOffset, selectedAmount)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDragMove(dragAmount)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.3f),
                ),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Icon + menu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        card.category?.let { cat ->
                            CategoryIcon(icon = cat.icon, color = cat.color, size = 28.dp)
                        } ?: run {
                            val emoji = if (card.type == TransactionType.EXPENSE) "💸" else "💰"
                            CategoryIcon(icon = emoji, color = "#9E9E9E", size = 28.dp)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_edit)) },
                                    onClick = { showMenu = false; onEdit() },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.action_delete),
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = { showMenu = false; onDelete() },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Card title
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Variants scrollable row
                    if (card.variants.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(vertical = 2.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            items(card.variants, key = { it.id }) { variant ->
                                val isSelected = variant.id == selectedVariantId
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) {
                                        accentColor
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    modifier = Modifier.clickable { selectedVariantId = variant.id },
                                ) {
                                    Text(
                                        text = CurrencyFormatter.formatCompact(variant.amount),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) {
                                            if (card.type == TransactionType.EXPENSE) {
                                                MaterialTheme.colorScheme.onError
                                            } else {
                                                MaterialTheme.colorScheme.onTertiary
                                            }
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Add button
                    Button(
                        onClick = { onAdd(selectedAmount) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.action_add),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Transaction item row
    // ---------------------------------------------------------------------------

    @Composable
    private fun TransactionItem(
        transaction: Transaction,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val isExpense = transaction.type == TransactionType.EXPENSE
        val accentColor = if (isExpense) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.tertiary
        }

        Box(modifier = modifier) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Left accent bar
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(accentColor),
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        transaction.category?.let { cat ->
                            CategoryIcon(icon = cat.icon, color = cat.color, size = 36.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                        } ?: run {
                            val emoji = if (isExpense) "💸" else "💰"
                            CategoryIcon(icon = emoji, color = "#9E9E9E", size = 36.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = transaction.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            transaction.note?.let { note ->
                                if (note.isNotBlank()) {
                                    Text(
                                        text = note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = (if (isExpense) "-" else "+") +
                                        CurrencyFormatter.formatCompact(transaction.amount),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                            )
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.action_edit),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.action_delete),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Shimmer skeleton for a transaction item
    // ---------------------------------------------------------------------------

    @Composable
    private fun ShimmerTransactionItem(modifier: Modifier = Modifier) {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        val shimmerOffset by infiniteTransition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = EaseInOut),
            ),
            label = "shimmerOffset",
        )

        val shimmerColors = listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.surfaceVariant,
        )

        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = shimmerColors,
                                    start = androidx.compose.ui.geometry.Offset(
                                        x = shimmerOffset * 400f,
                                        y = 0f,
                                    ),
                                    end = androidx.compose.ui.geometry.Offset(
                                        x = shimmerOffset * 400f + 400f,
                                        y = 0f,
                                    ),
                                ),
                            ),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = shimmerColors,
                                        start = androidx.compose.ui.geometry.Offset(
                                            x = shimmerOffset * 400f,
                                            y = 0f,
                                        ),
                                        end = androidx.compose.ui.geometry.Offset(
                                            x = shimmerOffset * 400f + 400f,
                                            y = 0f,
                                        ),
                                    ),
                                ),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.35f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = shimmerColors,
                                        start = androidx.compose.ui.geometry.Offset(
                                            x = shimmerOffset * 400f,
                                            y = 0f,
                                        ),
                                        end = androidx.compose.ui.geometry.Offset(
                                            x = shimmerOffset * 400f + 400f,
                                            y = 0f,
                                        ),
                                    ),
                                ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = shimmerColors,
                                    start = androidx.compose.ui.geometry.Offset(
                                        x = shimmerOffset * 400f,
                                        y = 0f,
                                    ),
                                    end = androidx.compose.ui.geometry.Offset(
                                        x = shimmerOffset * 400f + 400f,
                                        y = 0f,
                                    ),
                                ),
                            ),
                    )
                }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Total footer item
    // ---------------------------------------------------------------------------

    @Composable
    private fun TotalItem(
        label: String,
        amount: Long,
        color: Color,
        modifier: Modifier = Modifier,
    ) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = CurrencyFormatter.formatCompact(amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        }
    }

    // ---------------------------------------------------------------------------
    // Previews
    // ---------------------------------------------------------------------------

    @Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light")
    @Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark")
    @Composable
    private fun DayViewPreview() {
        DragSpendTheme {
            DayView(
                selectedDate = "2026-03-17",
                transactions = listOf(
                    Transaction(
                        id = "1",
                        userId = "u1",
                        date = "2026-03-17",
                        title = "Tiền xăng",
                        amount = 150_000,
                        type = TransactionType.EXPENSE,
                        position = 0,
                    ),
                ),
                dayTotal = com.bossxomlut.dragspend.data.model.DayTotal("2026-03-17", 0, 150_000),
                onDateChange = {},
                onEditTransaction = {},
                onDeleteTransaction = {},
                onCopyFromYesterday = {},
                isDragging = false,
                isDragOver = false,
            )
        }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Cards Panel – Light")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Cards Panel – Dark")
@Composable
private fun SpendingCardsPanelPreview() {
    DragSpendTheme {
        SpendingCardsPanel(
            cards = listOf(
                SpendingCard(
                    id = "1",
                    userId = "u1",
                    title = "Ăn sáng",
                    type = TransactionType.EXPENSE,
                    variants = listOf(
                        com.bossxomlut.dragspend.data.model.CardVariant(
                            id = "v1",
                            cardId = "1",
                            amount = 25_000,
                            isDefault = true,
                        ),
                        com.bossxomlut.dragspend.data.model.CardVariant(
                            id = "v2",
                            cardId = "1",
                            amount = 35_000,
                            isDefault = false,
                        ),
                    ),
                ),
                SpendingCard(
                    id = "2",
                    userId = "u1",
                    title = "Nhậu nhẹt",
                    type = TransactionType.EXPENSE,
                    variants = listOf(
                        com.bossxomlut.dragspend.data.model.CardVariant(
                            id = "v3",
                            cardId = "2",
                            amount = 100_000,
                            isDefault = true,
                        ),
                    ),
                ),
            ),
            searchQuery = "",
            onSearchChange = {},
            onCardAdd = { _, _ -> },
            onCardEdit = {},
            onCardDelete = {},
            onDragStart = { _, _, _ -> },
            onDragEnd = {},
            onDragMove = {},
            onAddCard = {},
        )
    }
}
