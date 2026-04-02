package com.bossxomlut.dragspend.ui.screen.dashboard.report

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt
import org.koin.androidx.compose.koinViewModel

@Composable
fun ReportScreen(
    dashboardViewModel: DashboardViewModel,
    isBalanceHidden: Boolean = false,
    onNavigateToDayDetail: (date: String) -> Unit = {},
    onNavigateToCategoryDetail: (yearMonth: String, categoryId: String, categoryName: String, categoryIcon: String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier,
    reportViewModel: ReportViewModel = koinViewModel(),
) {
    val uiState by reportViewModel.uiState.collectAsStateWithLifecycle()
    val viewMonth by dashboardViewModel.viewMonth.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var selectedCategorySlice by remember { mutableStateOf<CategorySlice?>(null) }
    var chartFilter by remember { mutableStateOf<ChartFilter>(ChartFilter.All) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val filteredBars = remember(uiState.dailyBars, uiState.categoryDailyBars, chartFilter) {
        when (val f = chartFilter) {
            is ChartFilter.All -> uiState.dailyBars
            is ChartFilter.ExpenseOnly -> uiState.dailyBars.map { it.copy(income = 0L) }
            is ChartFilter.IncomeOnly -> uiState.dailyBars.map { it.copy(expense = 0L) }
            is ChartFilter.ByCategoryFilter -> {
                val catAmounts = uiState.categoryDailyBars[f.categoryId] ?: emptyList()
                uiState.dailyBars.mapIndexed { i, bar ->
                    bar.copy(expense = catAmounts.getOrElse(i) { 0L }, income = 0L)
                }
            }
        }
    }

    LaunchedEffect(viewMonth) {
        selectedCategorySlice = null
        chartFilter = ChartFilter.All
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
            StatCards(uiState = uiState, isBalanceHidden = isBalanceHidden)

            NetBalanceBanner(
                totalIncome = uiState.totalIncome,
                totalExpense = uiState.totalExpense,
                isBalanceHidden = isBalanceHidden,
            )

            // Charts — shown when there is data, or while loading (old data stays visible)
            if (uiState.dailyBars.isNotEmpty() || uiState.isLoading) {
                ChartSectionCard(
                    title = stringResource(R.string.report_daily_chart_title),
                    trailingAction = {
                        ChartFilterButton(
                            filter = chartFilter,
                            onClick = { showFilterSheet = true },
                        )
                    },
                ) {
                    Spacer(modifier = Modifier.height(6.dp))
                    ChartLegendRow(filter = chartFilter)
                    Spacer(modifier = Modifier.height(8.dp))
                    DailyBarChart(
                        bars = filteredBars,
                        viewMonth = viewMonth,
                        onBarTap = { date -> onNavigateToDayDetail(date) },
                        primaryBarColor = (chartFilter as? ChartFilter.ByCategoryFilter)?.let { f ->
                            runCatching { Color(android.graphics.Color.parseColor(f.color)) }.getOrNull()
                        },
                        modifier = Modifier.fillMaxWidth(),
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
                                onSliceSelected = { selectedCategorySlice = it },
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
                        AnimatedVisibility(
                            visible = selectedCategorySlice != null,
                            enter = fadeIn(tween(150)) + expandVertically(),
                            exit = fadeOut(tween(150)) + shrinkVertically(),
                        ) {
                            selectedCategorySlice?.let { slice ->
                                val catId = slice.categoryId ?: "other"
                                val sliceColor = runCatching {
                                    Color(android.graphics.Color.parseColor(slice.color))
                                }.getOrDefault(Color.Gray)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            onNavigateToCategoryDetail(viewMonth, catId, slice.name, slice.icon)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = slice.icon,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Column {
                                            Text(
                                                text = slice.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = CurrencyFormatter.formatCompact(slice.amount),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = sliceColor,
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.report_view_category_transactions),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
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

        if (showFilterSheet) {
            ChartFilterBottomSheet(
                filter = chartFilter,
                categorySlices = uiState.categorySlices,
                hasIncome = uiState.totalIncome > 0,
                onFilterChange = {
                    chartFilter = it
                    showFilterSheet = false
                },
                onDismiss = { showFilterSheet = false },
            )
        }
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
private fun StatCards(uiState: ReportUiState, isBalanceHidden: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatCard(
                label = stringResource(R.string.report_total_expense),
                value = uiState.totalExpense,
                color = MaterialTheme.colorScheme.error,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                icon = "💸",
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.report_total_income),
                value = uiState.totalIncome,
                color = MaterialTheme.colorScheme.tertiary,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                icon = "💰",
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatCard(
                label = stringResource(R.string.report_avg_daily),
                value = uiState.avgDailyExpense,
                color = MaterialTheme.colorScheme.secondary,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                icon = "📅",
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.report_highest_day),
                value = uiState.highestExpenseDay,
                color = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                icon = "🔥",
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: Long,
    color: Color,
    containerColor: Color,
    icon: String,
    isBalanceHidden: Boolean = false,
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
                    text = if (isBalanceHidden) "••••••" else CurrencyFormatter.formatCompact(animatedValue.value.toLong()),
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
    isBalanceHidden: Boolean = false,
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
                    text = if (isBalanceHidden) "••••••" else (if (animatedNet.value >= 0) "+" else "") + CurrencyFormatter.formatCompact(animatedNet.value.toLong()),
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
    trailingAction: (@Composable () -> Unit)? = null,
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
            if (trailingAction != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    trailingAction()
                }
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            content()
        }
    }
}

@Composable
private fun ChartFilterButton(
    filter: ChartFilter,
    onClick: () -> Unit,
) {
    val isFiltered = filter !is ChartFilter.All
    val label = when (filter) {
        is ChartFilter.All -> stringResource(R.string.report_chart_filter_all)
        is ChartFilter.ExpenseOnly -> "💸 ${stringResource(R.string.report_legend_expense)}"
        is ChartFilter.IncomeOnly -> "💰 ${stringResource(R.string.report_legend_income)}"
        is ChartFilter.ByCategoryFilter -> "${filter.icon} ${filter.name}"
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isFiltered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                tint = if (isFiltered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isFiltered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isFiltered) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChartFilterBottomSheet(
    filter: ChartFilter,
    categorySlices: List<CategorySlice>,
    hasIncome: Boolean,
    onFilterChange: (ChartFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.report_chart_filter_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Type filters row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf<Triple<ChartFilter, String, Boolean>>(
                    Triple(ChartFilter.All, stringResource(R.string.report_chart_filter_all), true),
                    Triple(ChartFilter.ExpenseOnly, "💸 ${stringResource(R.string.report_legend_expense)}", true),
                    Triple(ChartFilter.IncomeOnly, "💰 ${stringResource(R.string.report_legend_income)}", hasIncome),
                ).filter { it.third }.forEach { (option, label, _) ->
                    val isSelected = when (option) {
                        is ChartFilter.All -> filter is ChartFilter.All
                        is ChartFilter.ExpenseOnly -> filter is ChartFilter.ExpenseOnly
                        is ChartFilter.IncomeOnly -> filter is ChartFilter.IncomeOnly
                        else -> false
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterChange(option) },
                        label = { Text(label, maxLines = 1) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (categorySlices.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.report_chart_filter_by_category),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(categorySlices) { slice ->
                        val catId = slice.categoryId ?: "other"
                        val isSelected = filter is ChartFilter.ByCategoryFilter &&
                            (filter as ChartFilter.ByCategoryFilter).categoryId == catId
                        val catColor = runCatching {
                            Color(android.graphics.Color.parseColor(slice.color))
                        }.getOrDefault(MaterialTheme.colorScheme.primary)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) catColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) BorderStroke(1.5.dp, catColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onFilterChange(
                                        if (isSelected) ChartFilter.All
                                        else ChartFilter.ByCategoryFilter(
                                            categoryId = catId,
                                            name = slice.name,
                                            icon = slice.icon,
                                            color = slice.color,
                                        ),
                                    )
                                },
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(slice.icon, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = slice.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
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
private fun ChartLegendRow(filter: ChartFilter = ChartFilter.All) {
    val expenseColor = MaterialTheme.colorScheme.error
    val incomeColor = MaterialTheme.colorScheme.primary
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (filter) {
            is ChartFilter.All -> {
                LegendDot(color = expenseColor, label = stringResource(R.string.report_legend_expense))
                LegendDot(color = incomeColor, label = stringResource(R.string.report_legend_income))
            }
            is ChartFilter.ExpenseOnly ->
                LegendDot(color = expenseColor, label = stringResource(R.string.report_legend_expense))
            is ChartFilter.IncomeOnly ->
                LegendDot(color = incomeColor, label = stringResource(R.string.report_legend_income))
            is ChartFilter.ByCategoryFilter -> {
                val catColor = runCatching {
                    Color(android.graphics.Color.parseColor(filter.color))
                }.getOrDefault(Color.Gray)
                LegendDot(color = catColor, label = "${filter.icon} ${filter.name}")
            }
        }
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
    viewMonth: String,
    onBarTap: (date: String) -> Unit,
    modifier: Modifier = Modifier,
    primaryBarColor: Color? = null,
) {
    val expenseColor = primaryBarColor ?: MaterialTheme.colorScheme.error
    val incomeColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxValue = bars.maxOfOrNull { maxOf(it.expense, it.income) }?.coerceAtLeast(1L) ?: 1L

    var selectedIndex by remember(bars) { mutableStateOf<Int?>(null) }
    val selectedBar = selectedIndex?.let { bars.getOrNull(it) }

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

    // Round maxValue up to the next multiple of a "nice" step:
    // - When leading digit is 1 (maxValue < 2×magnitude), use magnitude/10 as step → 125m → 130m
    // - Otherwise use magnitude as step → 26m → 30m, 55m → 60m
    val niceMax = remember(maxValue) {
        val magnitude = Math.pow(10.0, Math.floor(Math.log10(maxValue.toDouble()))).toLong()
        val step = if (maxValue < 2L * magnitude) maxOf(1L, magnitude / 10L) else magnitude
        val remainder = maxValue % step
        if (remainder == 0L) maxValue else maxValue + (step - remainder)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .pointerInput(bars) {
                    detectTapGestures(onTap = { tapOffset ->
                        if (bars.isEmpty()) return@detectTapGestures
                        val yAxisWidthPx = 40.dp.toPx()
                        val availableWidth = size.width - yAxisWidthPx
                        val barWidth = availableWidth / (bars.size * 3f)
                        val spacing = barWidth * 0.5f
                        var tappedIndex: Int? = null
                        bars.forEachIndexed { index, _ ->
                            val groupCenter = yAxisWidthPx + index * (barWidth * 2 + spacing * 2) + spacing + barWidth / 2f
                            val groupLeft = groupCenter - barWidth
                            val groupRight = groupCenter + barWidth + spacing * 1.5f
                            if (tapOffset.x in groupLeft..groupRight) {
                                tappedIndex = index
                            }
                        }
                        selectedIndex = if (tappedIndex == selectedIndex) null else tappedIndex
                    })
                },
        ) {
            val fraction = animFraction.value
            val yAxisWidthPx = 40.dp.toPx()
            val labelAreaHeight = 18.dp.toPx()
            val chartHeight = size.height - labelAreaHeight
            val availableWidth = size.width - yAxisWidthPx
            val barWidth = if (bars.isEmpty()) 0f else availableWidth / (bars.size * 3f)
            val spacing = barWidth * 0.5f
            val topPaddingPx = 20.dp.toPx()
            val maxBarHeight = chartHeight - 8f - topPaddingPx

            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)

            // Y-axis labels + grid lines
            val yAxisPaint = android.graphics.Paint().apply {
                color = labelColor
                textSize = 8.5f.dp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
                isAntiAlias = true
            }
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { level ->
                val y = chartHeight - level * maxBarHeight - 8f
                // Grid line across chart area
                drawLine(
                    color = gridColor,
                    start = Offset(yAxisWidthPx, y),
                    end = Offset(size.width, y),
                    strokeWidth = if (level == 1f) 1.5f else 1f,
                    pathEffect = if (level == 1f) null else dashEffect,
                )
                // Y-axis label
                val value = (niceMax * level).toLong()
                drawContext.canvas.nativeCanvas.drawText(
                    CurrencyFormatter.formatCompact(value),
                    yAxisWidthPx - 4.dp.toPx(),
                    y + yAxisPaint.textSize / 3f,
                    yAxisPaint,
                )
            }

            // Day labels + bars
            val dayLabelPaint = android.graphics.Paint().apply {
                color = labelColor
                textSize = 9.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }

            bars.forEachIndexed { index, bar ->
                val highlighted = selectedIndex == null || selectedIndex == index
                val alpha = if (highlighted) 1f else 0.3f
                val groupCenter = yAxisWidthPx + index * (barWidth * 2 + spacing * 2) + spacing + barWidth / 2f
                val cornerR = CornerRadius(barWidth / 2f, barWidth / 2f)

                val expenseHeight = (bar.expense.toFloat() / niceMax) * maxBarHeight * fraction
                if (expenseHeight > 0f) {
                    drawRoundRect(
                        color = expenseColor.copy(alpha = alpha),
                        topLeft = Offset(groupCenter - barWidth, chartHeight - expenseHeight - 8f),
                        size = Size(barWidth, expenseHeight),
                        cornerRadius = cornerR,
                    )
                }

                val incomeHeight = (bar.income.toFloat() / niceMax) * maxBarHeight * fraction
                if (incomeHeight > 0f) {
                    drawRoundRect(
                        color = incomeColor.copy(alpha = alpha),
                        topLeft = Offset(groupCenter + spacing * 0.5f, chartHeight - incomeHeight - 8f),
                        size = Size(barWidth, incomeHeight),
                        cornerRadius = cornerR,
                    )
                }

                val day = bar.day
                if (day == 1 || day % 5 == 0) {
                    drawContext.canvas.nativeCanvas.drawText(
                        day.toString(),
                        groupCenter,
                        size.height - 2.dp.toPx(),
                        dayLabelPaint,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = selectedBar != null,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
        ) {
            selectedBar?.let { bar ->
                val date = "$viewMonth-${bar.day.toString().padStart(2, '0')}"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceVariantColor, RoundedCornerShape(8.dp))
                        .clickable { onBarTap(date) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.report_day_label, bar.day),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = onSurfaceVariantColor,
                        )
                        Text(
                            text = "💸 ${CurrencyFormatter.formatCompact(bar.expense)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = expenseColor,
                        )
                        Text(
                            text = "💰 ${CurrencyFormatter.formatCompact(bar.income)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = incomeColor,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = onSurfaceVariantColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    slices: List<CategorySlice>,
    totalExpense: Long,
    onSliceSelected: (CategorySlice?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val total = slices.sumOf { it.amount }.toFloat().coerceAtLeast(1f)
    val expenseColor = MaterialTheme.colorScheme.error
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    var selectedSliceIndex by remember(slices) { mutableStateOf<Int?>(null) }
    val selectedSlice = selectedSliceIndex?.let { slices.getOrNull(it) }

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

    val animatedExpense = remember { Animatable(0f) }
    LaunchedEffect(totalExpense) {
        animatedExpense.animateTo(
            targetValue = totalExpense.toFloat(),
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        )
    }

    Box(
        modifier = modifier.pointerInput(slices) {
            detectTapGestures(onTap = { tapOffset ->
                val radius = minOf(size.width, size.height) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val strokeWidth = radius * 0.30f
                val outerRadius = radius - strokeWidth / 2f - 4f
                val innerRadius = outerRadius - strokeWidth
                val dx = tapOffset.x - center.x
                val dy = tapOffset.y - center.y
                val distFromCenter = sqrt(dx * dx + dy * dy)
                if (distFromCenter < innerRadius * 0.8f || distFromCenter > outerRadius + 12f) {
                    selectedSliceIndex = null
                    onSliceSelected(null)
                    return@detectTapGestures
                }
                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                if (angle < 0f) angle += 360f
                val gapAngle = 2.5f
                var accumulatedAngle = 0f
                var tappedIndex: Int? = null
                slices.forEachIndexed { index, slice ->
                    val sweep = ((slice.amount.toFloat() / total) * 360f) - gapAngle
                    if (angle >= accumulatedAngle && angle < accumulatedAngle + sweep + gapAngle) {
                        tappedIndex = index
                    }
                    accumulatedAngle += sweep + gapAngle
                }
                selectedSliceIndex = if (tappedIndex == selectedSliceIndex) null else tappedIndex
                onSliceSelected(selectedSliceIndex?.let { slices.getOrNull(it) })
            })
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val fraction = animFraction.value
            val radius = min(size.width, size.height) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val strokeWidth = radius * 0.30f
            val outerRadius = radius - strokeWidth / 2f - 4f
            var startAngle = -90f
            val gapAngle = 2.5f

            slices.forEachIndexed { index, slice ->
                val fullSweep = ((slice.amount.toFloat() / total) * 360f) - gapAngle
                val isHighlighted = selectedSliceIndex == null || selectedSliceIndex == index
                val color = runCatching {
                    Color(android.graphics.Color.parseColor(slice.color))
                }.getOrDefault(Color.Gray)

                drawArc(
                    color = color.copy(alpha = if (isHighlighted) 1f else 0.3f),
                    startAngle = startAngle,
                    sweepAngle = (fullSweep * fraction).coerceAtLeast(0f),
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = strokeWidth),
                )
                startAngle += fullSweep + gapAngle
            }
        }

        if (selectedSlice != null) {
            val sliceColor = runCatching {
                Color(android.graphics.Color.parseColor(selectedSlice.color))
            }.getOrDefault(Color.Gray)
            val percent = if (total > 0f) ((selectedSlice.amount.toFloat() / total) * 100f).toInt() else 0
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = "${selectedSlice.icon} ${selectedSlice.name}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = sliceColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = CurrencyFormatter.formatCompact(selectedSlice.amount),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = sliceColor,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariantColor,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
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
