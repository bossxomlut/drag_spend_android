package com.bossxomlut.dragspend.ui.screen.dashboard.today

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bossxomlut.dragspend.R
import com.bossxomlut.dragspend.data.model.Category
import com.bossxomlut.dragspend.data.model.SpendingCard
import com.bossxomlut.dragspend.data.model.Transaction
import com.bossxomlut.dragspend.data.model.TransactionType
import com.bossxomlut.dragspend.domain.repository.CreateTransactionRequest
import com.bossxomlut.dragspend.ui.components.AppToast
import com.bossxomlut.dragspend.ui.components.CategoryIcon
import com.bossxomlut.dragspend.ui.components.ConfirmDialog
import com.bossxomlut.dragspend.ui.components.ToastType
import com.bossxomlut.dragspend.ui.screen.dashboard.DashboardViewModel
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme
import com.bossxomlut.dragspend.util.CurrencyFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    date: String,
    dashboardViewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DayDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by dashboardViewModel.categories.collectAsStateWithLifecycle()
    val cards = emptyList<SpendingCard>()

    var editMode by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var editTransaction by remember { mutableStateOf<Transaction?>(null) }
    var deleteTransactionId by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(date) {
        viewModel.loadTransactions(date)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            toastMessage = it
            viewModel.clearError()
        }
    }

    val parsedDate = runCatching {
        LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }.getOrNull()

    val titleLabel = parsedDate?.let { d ->
        val dayName = d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val formatted = d.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
        "$dayName, $formatted"
    } ?: date

    val totalExpense = uiState.transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val totalIncome = uiState.transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = titleLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = pluralStringResource(
                                id = R.plurals.transaction_count,
                                count = uiState.transactions.size,
                                uiState.transactions.size,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { editMode = !editMode }) {
                        Icon(
                            imageVector = if (editMode) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = stringResource(
                                if (editMode) R.string.day_detail_disable_edit else R.string.day_detail_enable_edit,
                            ),
                            tint = if (editMode) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = editMode,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150)),
            ) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Summary row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SummaryChip(
                        label = stringResource(R.string.report_total_expense),
                        amount = totalExpense,
                        color = MaterialTheme.colorScheme.error,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryChip(
                        label = stringResource(R.string.report_total_income),
                        amount = totalIncome,
                        color = MaterialTheme.colorScheme.tertiary,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                }

                AnimatedVisibility(visible = uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                }

                AnimatedVisibility(
                    visible = editMode,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200)),
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.day_detail_edit_mode_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }

                if (uiState.isLoading && uiState.transactions.isEmpty()) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(4) { DayDetailShimmerItem() }
                    }
                } else if (uiState.transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.empty_transactions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(uiState.transactions, key = { it.id }) { transaction ->
                            DayDetailTransactionItem(
                                transaction = transaction,
                                editMode = editMode,
                                onEdit = { editTransaction = transaction },
                                onDelete = { deleteTransactionId = transaction.id },
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
                    .padding(top = 8.dp),
            )
        }
    }

    // Edit dialog
    editTransaction?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            categories = categories,
            cards = cards,
            onSave = { req ->
                viewModel.updateTransaction(tx.id, req)
                dashboardViewModel.markReportMonthDirty(date.substring(0, 7))
                dashboardViewModel.markDayDirty(date)
                editTransaction = null
            },
            onDismiss = { editTransaction = null },
            onCreateCategory = { name, icon, color, type ->
                dashboardViewModel.createCategory(name, icon, color, type)
            },
        )
    }

    // Delete confirm dialog
    deleteTransactionId?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.delete_transaction_title),
            message = stringResource(R.string.delete_transaction_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                viewModel.deleteTransaction(id)
                dashboardViewModel.markReportMonthDirty(date.substring(0, 7))
                dashboardViewModel.markDayDirty(date)
                deleteTransactionId = null
            },
            onDismiss = { deleteTransactionId = null },
            isDestructive = true,
        )
    }

    // Add dialog — reuse EditTransactionDialog with a blank transaction stub
    if (showAddDialog) {
        val blankTx = Transaction(
            id = "",
            userId = dashboardViewModel.currentUserId ?: "",
            date = date,
            title = "",
            amount = 0L,
            type = TransactionType.EXPENSE,
            position = 0,
        )
        EditTransactionDialog(
            transaction = blankTx,
            categories = categories,
            cards = cards,
            onSave = { req ->
                val userId = dashboardViewModel.currentUserId ?: return@EditTransactionDialog
                viewModel.addTransaction(
                    CreateTransactionRequest(
                        userId = userId,
                        sourceCardId = req.sourceCardId,
                        date = req.date,
                        title = req.title,
                        amount = req.amount,
                        categoryId = req.categoryId,
                        type = req.type,
                        note = req.note,
                    ),
                )
                dashboardViewModel.markReportMonthDirty(date.substring(0, 7))
                dashboardViewModel.markDayDirty(date)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
            onCreateCategory = { name, icon, color, type ->
                dashboardViewModel.createCategory(name, icon, color, type)
            },
        )
    }
}

@Composable
private fun SummaryChip(
    label: String,
    amount: Long,
    color: androidx.compose.ui.graphics.Color,
    containerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.75f),
            )
            Text(
                text = CurrencyFormatter.formatCompact(amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

@Composable
private fun DayDetailTransactionItem(
    transaction: Transaction,
    editMode: Boolean,
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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
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
                    AnimatedVisibility(
                        visible = editMode,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(150)),
                    ) {
                        Row {
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
}

@Composable
private fun DayDetailShimmerItem(modifier: Modifier = Modifier) {
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
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
                                start = androidx.compose.ui.geometry.Offset(shimmerOffset * 400f, 0f),
                                end = androidx.compose.ui.geometry.Offset(shimmerOffset * 400f + 400f, 0f),
                            ),
                        ),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = shimmerColors,
                                    start = androidx.compose.ui.geometry.Offset(shimmerOffset * 400f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(shimmerOffset * 400f + 400f, 0f),
                                ),
                            ),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = shimmerColors,
                                    start = androidx.compose.ui.geometry.Offset(shimmerOffset * 400f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(shimmerOffset * 400f + 400f, 0f),
                                ),
                            ),
                    )
                }
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.linearGradient(
                                colors = shimmerColors,
                                start = androidx.compose.ui.geometry.Offset(shimmerOffset * 400f, 0f),
                                end = androidx.compose.ui.geometry.Offset(shimmerOffset * 400f + 400f, 0f),
                            ),
                        ),
                )
            }
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light Mode")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
private fun DayDetailTransactionItemPreview() {
    DragSpendTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DayDetailTransactionItem(
                transaction = Transaction(
                    id = "1",
                    userId = "u1",
                    date = "2026-03-15",
                    title = "Tiền xăng",
                    amount = 150_000,
                    type = TransactionType.EXPENSE,
                    position = 0,
                    category = Category(
                        id = "c1",
                        userId = "u1",
                        name = "Xăng xe",
                        icon = "⛽",
                        color = "#F44336",
                        type = TransactionType.EXPENSE,
                    ),
                ),
                editMode = true,
                onEdit = {},
                onDelete = {},
            )
            DayDetailTransactionItem(
                transaction = Transaction(
                    id = "2",
                    userId = "u1",
                    date = "2026-03-15",
                    title = "Tiền trợ cấp",
                    amount = 5_000_000,
                    type = TransactionType.INCOME,
                    position = 1,
                ),
                editMode = false,
                onEdit = {},
                onDelete = {},
            )
        }
    }
}
