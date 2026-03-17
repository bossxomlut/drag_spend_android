package com.bossxomlut.dragspend.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme

@Composable
fun CategoryIcon(
    icon: String,
    color: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val bgColor = runCatching { Color(android.graphics.Color.parseColor(color)) }
        .getOrDefault(Color.Gray)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = icon,
            fontSize = (size.value * 0.5f).sp,
        )
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark")
@Composable
private fun CategoryIconPreview() {
    DragSpendTheme {
        CategoryIcon(icon = "🍜", color = "#FF5722")
    }
}
