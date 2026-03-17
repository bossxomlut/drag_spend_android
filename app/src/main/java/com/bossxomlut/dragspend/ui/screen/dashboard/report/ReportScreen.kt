package com.bossxomlut.dragspend.ui.screen.dashboard.report

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.bossxomlut.dragspend.ui.components.AppToast
import com.bossxomlut.dragspend.ui.components.ToastType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bossxomlut.dragspend.R
import com.bossxomlut.dragspend.ui.screen.dashboard.DashboardViewModel
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme
import com.bossxomlut.dragspend.util.CurrencyFormatter
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ReportScreen(
    dashboardViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    reportViewModel: ReportViewModel = koinViewModel(),
) {
    val uiState by reportViewModel.uiState.collectAsStateWithLifecycle()
    val viewMonth by dashboardViewModel.viewMonth.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewMonth) {
        reportViewModel.loadReport(viewMonth)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            toastMessage = it
            reportViewModel.clearError()
        }
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MonthNavigationHeader(
                viewMonth = viewMonth,
                onPrevious = {
                    val prev = YearMonth.parse(viewMonth).minusMonths(1)
                    dashboardViewModel.selectMonth(prev.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                },
                onNext = {
                    val next = YearMonth.parse(viewMonth).plusMonths(1)
                    dashboardViewModel.selectMonth(next.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                },
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.dailyBars.isEmpty()) {
                EmptyReportState()
            } else {
                StatCards(uiState = uiState)

                Text(
                    text = stringResource(R.string.report_daily_chart_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                DailyBarChart(
                    bars = uiState.dailyBars,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )

                if (uiState.categorySlices.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.report_category_chart_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        DonutChart(
                            slices = uiState.categorySlices,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                        CategoryLegend(
                            slices = uiState.categorySlices.take(6),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        AppToast(
            message = toastMessage,
            type = ToastType.ERROR,
            onDismiss = { toastMessage = null },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp),
        )
    }
}

@Composable
private fun MonthNavigationHeader(
    viewMonth: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val yearMonth = YearMonth.parse(viewMonth)
    val label = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year}"

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.action_previous_month),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = androidx.compose.ui.Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            IconButton(onClick = onNext) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.action_next_month),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun StatCards(uiState: ReportUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatCard(
            label = stringResource(R.string.report_total_expense),
            value = CurrencyFormatter.formatCompact(uiState.totalExpense),
            color = MaterialTheme.colorScheme.error,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            icon = "💸",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.report_total_income),
            value = CurrencyFormatter.formatCompact(uiState.totalIncome),
            color = MaterialTheme.colorScheme.tertiary,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            icon = "💰",
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatCard(
            label = stringResource(R.string.report_avg_daily),
            value = CurrencyFormatter.formatCompact(uiState.avgDailyExpense),
            color = MaterialTheme.colorScheme.secondary,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            icon = "📅",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.report_highest_day),
            value = CurrencyFormatter.formatCompact(uiState.highestExpenseDay),
            color = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            icon = "🔥",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    containerColor: Color,
    icon: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(containerColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(icon, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

@Composable
private fun DailyBarChart(
    bars: List<DailyBarData>,
    modifier: Modifier = Modifier,
) {
    val expenseColor = MaterialTheme.colorScheme.error
    val incomeColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.outlineVariant
    val maxValue = bars.maxOf { maxOf(it.expense, it.income) }.coerceAtLeast(1L)

    Canvas(modifier = modifier) {
        val barWidth = size.width / (bars.size * 2.5f)
        val spacing = barWidth * 0.5f
        val maxBarHeight = size.height - 20f

        bars.forEachIndexed { index, bar ->
            val xBase = index * (barWidth * 2 + spacing * 2) + spacing

            val expenseHeight = (bar.expense.toFloat() / maxValue) * maxBarHeight
            drawRect(
                color = expenseColor,
                topLeft = Offset(xBase, size.height - expenseHeight - 16f),
                size = Size(barWidth, expenseHeight),
            )

            val incomeHeight = (bar.income.toFloat() / maxValue) * maxBarHeight
            drawRect(
                color = incomeColor,
                topLeft = Offset(xBase + barWidth + spacing * 0.5f, size.height - incomeHeight - 16f),
                size = Size(barWidth, incomeHeight),
            )
        }
    }
}

@Composable
private fun DonutChart(
    slices: List<CategorySlice>,
    modifier: Modifier = Modifier,
) {
    val total = slices.sumOf { it.amount }.toFloat().coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val strokeWidth = radius * 0.35f
        val outerRadius = radius - strokeWidth / 2f
        var startAngle = -90f

        slices.forEach { slice ->
            val sweep = (slice.amount.toFloat() / total) * 360f
            val color = runCatching {
                Color(android.graphics.Color.parseColor(slice.color))
            }.getOrDefault(Color.Gray)

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun CategoryLegend(
    slices: List<CategorySlice>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        slices.forEach { slice ->
            val color = runCatching {
                Color(android.graphics.Color.parseColor(slice.color))
            }.getOrDefault(Color.Gray)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .then(Modifier.also {  }),
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = color)
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${slice.icon} ${slice.name}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = CurrencyFormatter.formatCompact(slice.amount),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun EmptyReportState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.report_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark")
@Composable
private fun ReportScreenPreview() {
    DragSpendTheme {
        Column(Modifier.padding(16.dp)) {
            StatCards(
                uiState = ReportUiState(
                    totalExpense = 1_500_000,
                    totalIncome = 20_000_000,
                    avgDailyExpense = 50_000,
                    highestExpenseDay = 300_000,
                ),
            )
        }
    }
}
