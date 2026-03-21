package com.bossxomlut.dragspend.ui.screen.dashboard.report

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bossxomlut.dragspend.R
import com.bossxomlut.dragspend.ui.components.AppToast
import com.bossxomlut.dragspend.ui.components.ToastType
import com.bossxomlut.dragspend.ui.screen.dashboard.DashboardViewModel
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme
import com.bossxomlut.dragspend.util.CurrencyFormatter
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.min
import org.koin.androidx.compose.koinViewModel

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
        // If the Today screen mutated data for this month, bypass the cache and fetch fresh.
        if (viewMonth in dashboardViewModel.dirtyReportMonths.value) {
            reportViewModel.invalidateAndReload(viewMonth)
            dashboardViewModel.clearDirtyReportMonth(viewMonth)
        } else {
            reportViewModel.loadReport(viewMonth)
        }
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
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

            // Thin non-blocking loading bar — content stays visible while data updates
            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(300)),
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                )
            }

            // Stats — always rendered; values animate smoothly when month changes
            StatCards(uiState = uiState)

            NetBalanceBanner(
                totalIncome = uiState.totalIncome,
                totalExpense = uiState.totalExpense,
            )

            // Charts — shown when there is data, or while loading (old data stays visible)
            if (uiState.dailyBars.isNotEmpty() || uiState.isLoading) {
                ChartSectionCard(
                    title = stringResource(R.string.report_daily_chart_title),
                ) {
                    Spacer(modifier = Modifier.height(6.dp))
                    ChartLegendRow()
                    Spacer(modifier = Modifier.height(8.dp))
                    DailyBarChart(
                        bars = uiState.dailyBars,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    )
                }

                if (uiState.categorySlices.isNotEmpty()) {
                    ChartSectionCard(
                        title = stringResource(R.string.report_category_chart_title),
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DonutChart(
                                slices = uiState.categorySlices,
                                totalExpense = uiState.totalExpense,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                            )
                            CategoryLegend(
                                slices = uiState.categorySlices.take(6),
                                total = uiState.totalExpense,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            } else {
                EmptyReportState()
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

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
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
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
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
            value = uiState.totalExpense,
            color = MaterialTheme.colorScheme.error,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            icon = "💸",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.report_total_income),
            value = uiState.totalIncome,
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
            value = uiState.avgDailyExpense,
            color = MaterialTheme.colorScheme.secondary,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            icon = "📅",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.report_highest_day),
            value = uiState.highestExpenseDay,
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
    value: Long,
    color: Color,
    containerColor: Color,
    icon: String,
    modifier: Modifier = Modifier,
) {
    val animatedValue = remember { Animatable(0f) }
    LaunchedEffect(value) {
        animatedValue.animateTo(
            targetValue = value.toFloat(),
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        )
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(containerColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(icon, style = MaterialTheme.typography.titleLarge)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = CurrencyFormatter.formatCompact(animatedValue.value.toLong()),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun NetBalanceBanner(
    totalIncome: Long,
    totalExpense: Long,
) {
    val net = totalIncome - totalExpense
    val isPositive = net >= 0
    val animatedNet = remember { Animatable(0f) }
    LaunchedEffect(net) {
        animatedNet.animateTo(
            targetValue = net.toFloat(),
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        )
    }
    val containerColor = if (isPositive) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (isPositive) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.error
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (isPositive) "📈" else "📉",
                style = MaterialTheme.typography.titleLarge,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.report_net_balance),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.75f),
                )
                Text(
                    text = (if (animatedNet.value >= 0) "+" else "") + CurrencyFormatter.formatCompact(animatedNet.value.toLong()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
            }
        }
    }
}

@Composable
private fun ChartSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
    }
}

@Composable
private fun ChartLegendRow() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendDot(
            color = MaterialTheme.colorScheme.error,
            label = stringResource(R.string.report_legend_expense),
        )
        LegendDot(
            color = MaterialTheme.colorScheme.primary,
            label = stringResource(R.string.report_legend_income),
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DailyBarChart(
    bars: List<DailyBarData>,
    modifier: Modifier = Modifier,
) {
    val expenseColor = MaterialTheme.colorScheme.error
    val incomeColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val maxValue = bars.maxOfOrNull { maxOf(it.expense, it.income) }?.coerceAtLeast(1L) ?: 1L

    // Animatable fraction 0→1; snaps to 0 and grows to 1 each time bars data arrives
    val animFraction = remember { Animatable(1f) }
    LaunchedEffect(bars) {
        if (bars.isNotEmpty()) {
            animFraction.snapTo(0f)
            animFraction.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            )
        }
    }

    Canvas(modifier = modifier) {
        val fraction = animFraction.value
        val labelAreaHeight = 18.dp.toPx()
        val chartHeight = size.height - labelAreaHeight
        val barWidth = if (bars.isEmpty()) 0f else size.width / (bars.size * 2.5f)
        val spacing = barWidth * 0.5f
        val maxBarHeight = chartHeight - 8f

        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
        listOf(0.25f, 0.5f, 0.75f).forEach { level ->
            val y = chartHeight - level * maxBarHeight - 8f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
                pathEffect = dashEffect,
            )
        }

        val paint = android.graphics.Paint().apply {
            color = labelColor
            textSize = 9.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        bars.forEachIndexed { index, bar ->
            val groupCenter = index * (barWidth * 2 + spacing * 2) + spacing + barWidth / 2f
            val cornerR = CornerRadius(barWidth / 2f, barWidth / 2f)

            val expenseHeight = (bar.expense.toFloat() / maxValue) * maxBarHeight * fraction
            if (expenseHeight > 0f) {
                drawRoundRect(
                    color = expenseColor,
                    topLeft = Offset(groupCenter - barWidth, chartHeight - expenseHeight - 8f),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius = cornerR,
                )
            }

            val incomeHeight = (bar.income.toFloat() / maxValue) * maxBarHeight * fraction
            if (incomeHeight > 0f) {
                drawRoundRect(
                    color = incomeColor,
                    topLeft = Offset(groupCenter + spacing * 0.5f, chartHeight - incomeHeight - 8f),
                    size = Size(barWidth, incomeHeight),
                    cornerRadius = cornerR,
                )
            }

            // Draw day label — show every 5th day (1, 5, 10, 15, 20, 25, 30) + last day
            val day = bar.day
            if (day == 1 || day % 5 == 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    day.toString(),
                    groupCenter,
                    size.height - 2.dp.toPx(),
                    paint,
                )
            }
        }
    }
}

@Composable
private fun DonutChart(
    slices: List<CategorySlice>,
    totalExpense: Long,
    modifier: Modifier = Modifier,
) {
    val total = slices.sumOf { it.amount }.toFloat().coerceAtLeast(1f)
    val expenseColor = MaterialTheme.colorScheme.error

    // Arc reveal: all slices grow from their start angle simultaneously
    val animFraction = remember { Animatable(1f) }
    LaunchedEffect(slices) {
        if (slices.isNotEmpty()) {
            animFraction.snapTo(0f)
            animFraction.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            )
        }
    }

    // Center total counts up/down when value changes
    val animatedExpense = remember { Animatable(0f) }
    LaunchedEffect(totalExpense) {
        animatedExpense.animateTo(
            targetValue = totalExpense.toFloat(),
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val fraction = animFraction.value
            val radius = min(size.width, size.height) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val strokeWidth = radius * 0.30f
            val outerRadius = radius - strokeWidth / 2f - 4f
            var startAngle = -90f
            val gapAngle = 2.5f

            slices.forEach { slice ->
                val fullSweep = ((slice.amount.toFloat() / total) * 360f) - gapAngle
                val color = runCatching {
                    Color(android.graphics.Color.parseColor(slice.color))
                }.getOrDefault(Color.Gray)

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = (fullSweep * fraction).coerceAtLeast(0f),
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = strokeWidth),
                )
                // Advance start angle by full sweep so each arc starts at its correct position
                startAngle += fullSweep + gapAngle
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "💸",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = CurrencyFormatter.formatCompact(animatedExpense.value.toLong()),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = expenseColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CategoryLegend(
    slices: List<CategorySlice>,
    total: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        slices.forEach { slice ->
            val color = runCatching {
                Color(android.graphics.Color.parseColor(slice.color))
            }.getOrDefault(Color.Gray)
            val percent = if (total > 0) (slice.amount * 100f / total).toInt() else 0

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color, CircleShape),
                )
                Text(
                    text = "${slice.icon} ${slice.name}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyReportState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "🗓️",
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = stringResource(R.string.report_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark")
@Composable
private fun ReportScreenPreview() {
    DragSpendTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatCards(
                uiState = ReportUiState(
                    totalExpense = 1_500_000,
                    totalIncome = 20_000_000,
                    avgDailyExpense = 50_000,
                    highestExpenseDay = 300_000,
                ),
            )
            NetBalanceBanner(totalIncome = 20_000_000, totalExpense = 1_500_000)
        }
    }
}
