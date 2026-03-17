package com.bossxomlut.dragspend.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.bossxomlut.dragspend.R
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme
import com.bossxomlut.dragspend.util.CurrencyFormatter

@Composable
fun AmountTextField(
    value: String,
    onValueChange: (raw: String, parsed: Long?) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
) {
    val isError = value.isNotEmpty() && !CurrencyFormatter.isValidAmountInput(value)

    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val parsed = CurrencyFormatter.parseCompact(raw)
            onValueChange(raw, parsed)
        },
        label = label?.let { { Text(it) } },
        placeholder = { Text("0") },
        singleLine = true,
        isError = isError,
        supportingText = if (isError) {
            { Text(stringResource(R.string.error_invalid_amount)) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(
            onDone = { onImeAction?.invoke() },
            onNext = { onImeAction?.invoke() },
        ),
        modifier = modifier,
    )
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark")
@Composable
private fun AmountTextFieldPreview() {
    DragSpendTheme {
        var text by remember { mutableStateOf("25k") }
        AmountTextField(
            value = text,
            onValueChange = { raw, _ -> text = raw },
            label = "Amount",
        )
    }
}
