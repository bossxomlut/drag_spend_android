package com.bossxomlut.dragspend.ui.screen.dashboard.today

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bossxomlut.dragspend.R
import com.bossxomlut.dragspend.data.model.Category
import com.bossxomlut.dragspend.data.model.SpendingCard
import com.bossxomlut.dragspend.data.model.TransactionType
import com.bossxomlut.dragspend.domain.repository.CreateCardRequest
import com.bossxomlut.dragspend.domain.repository.CreateVariantRequest
import com.bossxomlut.dragspend.ui.components.AmountTextField
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme
import com.bossxomlut.dragspend.util.CurrencyFormatter

data class VariantDraft(
    val label: String = "",
    val amountText: String = "",
    val isDefault: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCardDialog(
    userId: String,
    language: String,
    categories: List<Category>,
    editCard: SpendingCard? = null,
    onSave: (CreateCardRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        CreateCardContent(
            userId = userId,
            language = language,
            categories = categories,
            editCard = editCard,
            onSave = onSave,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun CreateCardContent(
    userId: String,
    language: String,
    categories: List<Category>,
    editCard: SpendingCard?,
    onSave: (CreateCardRequest) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf(editCard?.title ?: "") }
    var selectedType by remember { mutableStateOf(editCard?.type ?: TransactionType.EXPENSE) }
    var selectedCategoryId by remember { mutableStateOf(editCard?.categoryId) }
    var note by remember { mutableStateOf(editCard?.note ?: "") }
    val variants = remember {
        mutableStateListOf<VariantDraft>().apply {
            if (editCard != null) {
                addAll(editCard.variants.map { v ->
                    VariantDraft(
                        label = v.label ?: "",
                        amountText = CurrencyFormatter.formatCompact(v.amount),
                        isDefault = v.isDefault,
                    )
                })
            }
            if (isEmpty()) add(VariantDraft(isDefault = true))
        }
    }

    val isValid = title.isNotBlank() && variants.all { v ->
        CurrencyFormatter.parseCompact(v.amountText) != null
    }

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
                text = if (editCard == null) stringResource(R.string.create_card_title) else stringResource(R.string.edit_card_title),
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.label_title)) },
                singleLine = true,
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
                    categories = categories.filter { it.type == selectedType },
                    selectedId = selectedCategoryId,
                    onSelect = { selectedCategoryId = it },
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.label_note)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.label_variants),
                style = MaterialTheme.typography.labelLarge,
            )

            variants.forEachIndexed { index, variant ->
                VariantRow(
                    variant = variant,
                    showDelete = variants.size > 1,
                    onUpdate = { updated -> variants[index] = updated },
                    onDelete = { variants.removeAt(index) },
                    onSetDefault = {
                        val currentDefault = variants.indexOfFirst { it.isDefault }
                        if (currentDefault >= 0 && currentDefault != index) {
                            variants[currentDefault] = variants[currentDefault].copy(isDefault = false)
                        }
                        variants[index] = variants[index].copy(isDefault = true)
                    },
                )
            }

            TextButton(
                onClick = { variants.add(VariantDraft()) },
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(stringResource(R.string.action_add_variant))
            }

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
                    val request = CreateCardRequest(
                        userId = userId,
                        title = title,
                        categoryId = selectedCategoryId,
                        type = selectedType,
                        note = note.ifBlank { null },
                        language = language,
                        variants = variants.mapIndexed { i, v ->
                            CreateVariantRequest(
                                label = v.label.ifBlank { null },
                                amount = CurrencyFormatter.parseCompact(v.amountText) ?: 0L,
                                isDefault = v.isDefault,
                                position = i,
                            )
                        },
                    )
                    onSave(request)
                },
                enabled = isValid,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun VariantRow(
    variant: VariantDraft,
    showDelete: Boolean,
    onUpdate: (VariantDraft) -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = variant.label,
            onValueChange = { onUpdate(variant.copy(label = it)) },
            label = { Text(stringResource(R.string.label_variant_label)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        AmountTextField(
            value = variant.amountText,
            onValueChange = { raw, _ -> onUpdate(variant.copy(amountText = raw)) },
            modifier = Modifier.weight(1f),
        )
        Checkbox(
            checked = variant.isDefault,
            onCheckedChange = { if (it) onSetDefault() },
        )
        if (showDelete) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark")
@Composable
private fun CreateCardDialogPreview() {
    DragSpendTheme {
        CreateCardContent(
            userId = "user1",
            language = "vi",
            categories = emptyList(),
            editCard = null,
            onSave = {},
            onDismiss = {},
        )
    }
}
