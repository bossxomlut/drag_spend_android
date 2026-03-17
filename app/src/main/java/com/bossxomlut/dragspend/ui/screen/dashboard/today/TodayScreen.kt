package com.bossxomlut.dragspend.ui.screen.dashboard.today

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.bossxomlut.dragspend.ui.components.AppToast
import com.bossxomlut.dragspend.ui.components.ToastType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bossxomlut.dragspend.R
import com.bossxomlut.dragspend.data.model.SpendingCard
import com.bossxomlut.dragspend.data.model.Transaction
import com.bossxomlut.dragspend.data.model.TransactionType
import com.bossxomlut.dragspend.ui.components.CategoryIcon
import com.bossxomlut.dragspend.ui.components.ConfirmDialog
import com.bossxomlut.dragspend.ui.screen.dashboard.DashboardViewModel
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme
import com.bossxomlut.dragspend.util.CurrencyFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TodayScreen(
    dashboardViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    todayViewModel: TodayViewModel = koinViewModel(),
) {
    val uiState by todayViewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by dashboardViewModel.selectedDate.collectAsStateWithLifecycle()
    val categories by dashboardViewModel.categories.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val userId = dashboardViewModel.currentUserId ?: ""
    val language = "vi"

    LaunchedEffect(selectedDate) {
        todayViewModel.loadData(selectedDate)
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
    var searchQuery by remember { mutableStateOf("") }

    val filteredCards by remember(uiState.cards, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) uiState.cards
            else uiState.cards.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Drag state
    var draggingCard by remember { mutableStateOf<SpendingCard?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dayViewBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateCard = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_new_card))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Top half — Card Panel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp),
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.placeholder_search_cards)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )

                val expenseCards = filteredCards.filter { it.type == TransactionType.EXPENSE }
                val incomeCards = filteredCards.filter { it.type == TransactionType.INCOME }

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (expenseCards.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.error, CircleShape),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.type_expense),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        items(expenseCards, key = { it.id }) { card ->
                            CardItem(
                                card = card,
                                onTap = { todayViewModel.addTransactionFromCard(card, selectedDate) },
                                onEdit = { editCard = card },
                                onDelete = { deleteCardId = card.id },
                                onDragStart = { draggingCard = card },
                                onDragEnd = { offset ->
                                    val bounds = dayViewBounds
                                    if (bounds != null && bounds.contains(offset)) {
                                        todayViewModel.addTransactionFromCard(card, selectedDate)
                                    }
                                    draggingCard = null
                                    dragOffset = Offset.Zero
                                },
                                onDragMove = { delta -> dragOffset += delta },
                            )
                        }
                    }

                    if (incomeCards.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.type_income),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }
                        items(incomeCards, key = { it.id }) { card ->
                            CardItem(
                                card = card,
                                onTap = { todayViewModel.addTransactionFromCard(card, selectedDate) },
                                onEdit = { editCard = card },
                                onDelete = { deleteCardId = card.id },
                                onDragStart = { draggingCard = card },
                                onDragEnd = { offset ->
                                    val bounds = dayViewBounds
                                    if (bounds != null && bounds.contains(offset)) {
                                        todayViewModel.addTransactionFromCard(card, selectedDate)
                                    }
                                    draggingCard = null
                                    dragOffset = Offset.Zero
                                },
                                onDragMove = { delta -> dragOffset += delta },
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Bottom half — Day View
            DayView(
                selectedDate = selectedDate,
                transactions = uiState.transactions,
                dayTotal = uiState.dayTotal,
                onDateChange = { dashboardViewModel.selectDate(it) },
                onEditTransaction = { editTransaction = it },
                onDeleteTransaction = { deleteTransactionId = it.id },
                onCopyFromYesterday = { todayViewModel.copyFromYesterday(selectedDate) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        dayViewBounds = coords.boundsInRoot()
                    },
            )
        }

        // Floating drag ghost
        if (draggingCard != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                    .padding(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                ) {
                    Text(
                        text = draggingCard!!.title,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
                deleteTransactionId = null
            },
            onDismiss = { deleteTransactionId = null },
            isDestructive = true,
        )
    }
}

@Composable
private fun CardItem(
    card: SpendingCard,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    val accentColor = if (card.type == TransactionType.EXPENSE) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    Box(modifier = modifier) {
        Card(
            onClick = onTap,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(card.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            dragPosition = offset
                            onDragStart()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragPosition += dragAmount
                            onDragMove(dragAmount)
                        },
                        onDragEnd = { onDragEnd(dragPosition) },
                        onDragCancel = { onDragEnd(dragPosition) },
                    )
                },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Colored left accent strip
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(accentColor),
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    card.category?.let { cat ->
                        CategoryIcon(icon = cat.icon, color = cat.color, size = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (card.variants.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                card.variants.take(3).forEach { variant ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = accentColor.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(6.dp),
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = CurrencyFormatter.formatCompact(variant.amount),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accentColor,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Text("⋮", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_edit)) },
                onClick = { showMenu = false; onEdit() },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; onDelete() },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            )
        }
    }
}

@Composable
private fun DayView(
    selectedDate: String,
    transactions: List<Transaction>,
    dayTotal: com.bossxomlut.dragspend.data.model.DayTotal,
    onDateChange: (String) -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onCopyFromYesterday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.parse(selectedDate, dateFormatter)
    val weekDays = (-3..3).map { today.plusDays(it.toLong()) }

    Column(modifier = modifier) {
        // Date navigation header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                onDateChange(today.minusDays(1).format(dateFormatter))
            }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = stringResource(R.string.action_previous_day))
            }
            Text(
                text = today.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = {
                onDateChange(today.plusDays(1).format(dateFormatter))
            }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = stringResource(R.string.action_next_day))
            }
        }

        // Week strip
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            items(weekDays, key = { it.toString() }) { day ->
                val isSelected = day == today
                val isToday = day == LocalDate.now()
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else androidx.compose.ui.graphics.Color.Transparent,
                            shape = CircleShape,
                        )
                        .then(
                            if (isToday && !isSelected) {
                                Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier
                            },
                        )
                        .then(
                            Modifier.background(
                                androidx.compose.ui.graphics.Color.Transparent,
                                CircleShape,
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        color = androidx.compose.ui.graphics.Color.Transparent,
                        onClick = { onDateChange(day.format(dateFormatter)) },
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(
                                text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = day.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        // Transactions list
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(transactions, key = { it.id }) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    onEdit = { onEditTransaction(transaction) },
                    onDelete = { onDeleteTransaction(transaction) },
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.empty_transactions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                TextButton(
                    onClick = onCopyFromYesterday,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_copy_from_yesterday))
                }
            }
        }

        // Daily total footer
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TotalItem(
                    label = stringResource(R.string.label_income),
                    amount = dayTotal.income,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                )
                TotalItem(
                    label = stringResource(R.string.label_expense),
                    amount = dayTotal.expense,
                    color = MaterialTheme.colorScheme.error,
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                )
                TotalItem(
                    label = stringResource(R.string.label_net),
                    amount = dayTotal.net,
                    color = if (dayTotal.net >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun TotalItem(
    label: String,
    amount: Long,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = CurrencyFormatter.formatCompact(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val isExpense = transaction.type == TransactionType.EXPENSE
    val accentColor = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

    Box(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Colored left border
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(accentColor),
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    transaction.category?.let { cat ->
                        CategoryIcon(icon = cat.icon, color = cat.color, size = 32.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    } ?: run {
                        val emoji = if (isExpense) "💸" else "💰"
                        CategoryIcon(icon = emoji, color = "#9E9E9E", size = 32.dp)
                        Spacer(modifier = Modifier.width(8.dp))
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
                    Text(
                        text = (if (isExpense) "-" else "+") + CurrencyFormatter.formatCompact(transaction.amount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                    )
                    IconButton(onClick = { showMenu = true }) {
                        Text("⋮", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_edit)) },
                onClick = { showMenu = false; onEdit() },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; onDelete() },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            )
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark")
@Composable
private fun DayViewPreview() {
    DragSpendTheme {
        DayView(
            selectedDate = "2026-03-16",
            transactions = listOf(
                Transaction(
                    id = "1",
                    userId = "u1",
                    date = "2026-03-16",
                    title = "Cơm trưa",
                    amount = 35_000,
                    type = TransactionType.EXPENSE,
                    position = 0,
                ),
                Transaction(
                    id = "2",
                    userId = "u1",
                    date = "2026-03-16",
                    title = "Lương",
                    amount = 20_000_000,
                    type = TransactionType.INCOME,
                    position = 1,
                ),
            ),
            dayTotal = com.bossxomlut.dragspend.data.model.DayTotal("2026-03-16", 20_000_000, 35_000),
            onDateChange = {},
            onEditTransaction = {},
            onDeleteTransaction = {},
            onCopyFromYesterday = {},
        )
    }
}
