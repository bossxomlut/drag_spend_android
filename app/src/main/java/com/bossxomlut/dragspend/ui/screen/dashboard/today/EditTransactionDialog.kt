package com.bossxomlut.dragspend.ui.screen.dashboard.today

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bossxomlut.dragspend.R
import com.bossxomlut.dragspend.data.model.Category
import com.bossxomlut.dragspend.data.model.SpendingCard
import com.bossxomlut.dragspend.data.model.Transaction
import com.bossxomlut.dragspend.data.model.TransactionType
import com.bossxomlut.dragspend.domain.repository.UpdateTransactionRequest
import com.bossxomlut.dragspend.ui.components.AmountTextField
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme
import com.bossxomlut.dragspend.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    categories: List<Category>,
    cards: List<SpendingCard>,
    onSave: (UpdateTransactionRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        EditTransactionContent(
            initial = transaction,
            categories = categories,
            cards = cards,
            onSave = onSave,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun EditTransactionContent(
    initial: Transaction,
    categories: List<Category>,
    cards: List<SpendingCard>,
    onSave: (UpdateTransactionRequest) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf(initial.title) }
    var amountText by remember { mutableStateOf(CurrencyFormatter.formatCompact(initial.amount)) }
    var parsedAmount by remember { mutableStateOf<Long?>(initial.amount) }
    var selectedType by remember { mutableStateOf(initial.type) }
    var selectedCategoryId by remember { mutableStateOf(initial.categoryId) }
    var selectedCardId by remember { mutableStateOf(initial.sourceCardId) }
    var note by remember { mutableStateOf(initial.note ?: "") }

    val isValid = title.isNotBlank() && parsedAmount != null && parsedAmount!! > 0

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // ── Scrollable form content ────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.edit_transaction_title),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.label_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            AmountTextField(
                value = amountText,
                onValueChange = { raw, parsed ->
                    amountText = raw
                    parsedAmount = parsed
                },
                label = stringResource(R.string.label_amount),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { selectedType = TransactionType.EXPENSE },
                    label = { Text(stringResource(R.string.type_expense)) },
                )
                FilterChip(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { selectedType = TransactionType.INCOME },
                    label = { Text(stringResource(R.string.type_income)) },
                )
            }

            if (categories.isNotEmpty()) {
                CategorySelector(
                    categories = categories,
                    selectedId = selectedCategoryId,
                    onSelect = { selectedCategoryId = it },
                )
            }

            if (cards.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.label_source_card),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SpendingCardSelector(
                    cards = cards,
                    selectedCardId = selectedCardId,
                    onSelect = { selectedCardId = it },
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.label_note)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))
        }

        // ── Sticky action buttons ──────────────────────────────────────────
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                onClick = {
                    onSave(
                        UpdateTransactionRequest(
                            title = title,
                            amount = parsedAmount ?: 0L,
                            categoryId = selectedCategoryId,
                            type = selectedType,
                            note = note.ifBlank { null },
                            date = initial.date,
                            sourceCardId = selectedCardId,
                        ),
                    )
                },
                enabled = isValid,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpendingCardSelector(
    cards: List<SpendingCard>,
    selectedCardId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedCardId == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.label_no_card)) },
        )
        cards.forEach { card ->
            FilterChip(
                selected = selectedCardId == card.id,
                onClick = { onSelect(if (selectedCardId == card.id) null else card.id) },
                label = { Text(card.title) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategorySelector(
    categories: List<Category>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filtered = categories.takeLast(10)
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filtered.forEach { cat ->
            FilterChip(
                selected = selectedId == cat.id,
                onClick = {
                    onSelect(if (selectedId == cat.id) null else cat.id)
                },
                label = { Text("${cat.icon} ${cat.name}") },
            )
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark")
@Composable
private fun EditTransactionDialogPreview() {
    DragSpendTheme {
        val tx = Transaction(
            id = "1",
            userId = "u1",
            date = "2026-03-16",
            title = "Cơm trưa",
            amount = 35_000,
            type = TransactionType.EXPENSE,
            position = 0,
        )
        EditTransactionContent(
            initial = tx,
            categories = emptyList(),
            cards = emptyList(),
            onSave = {},
            onDismiss = {},
        )
    }
}
