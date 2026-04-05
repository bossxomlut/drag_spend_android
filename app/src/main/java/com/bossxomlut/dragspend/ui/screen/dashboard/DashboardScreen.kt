package com.bossxomlut.dragspend.ui.screen.dashboard

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.bossxomlut.dragspend.R
import com.bossxomlut.dragspend.ui.screen.dashboard.report.ReportScreen
import com.bossxomlut.dragspend.ui.screen.dashboard.today.TodayScreen
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme

private data class DashboardTab(
    val label: @Composable () -> String,
    val icon: ImageVector,
)

@Composable
fun DashboardScreen(
    language: String,
    onSignOut: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDayDetail: (date: String) -> Unit = {},
    onNavigateToCategoryDetail: (yearMonth: String, categoryId: String, categoryName: String, categoryIcon: String) -> Unit = { _, _, _, _ -> },
    onNavigateToSearch: () -> Unit = {},
    dashboardViewModel: DashboardViewModel = koinViewModel(),
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf(
        DashboardTab(
            label = { stringResource(R.string.tab_today) },
            icon = Icons.Default.DateRange,
        ),
        DashboardTab(
            label = { stringResource(R.string.tab_report) },
            icon = Icons.Default.PieChart,
        ),
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                ) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label(),
                                    tint = if (selectedTab == index) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            },
                            label = {
                                Text(
                                    tab.label(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                )
            }
        },
    ) { padding ->
        when (selectedTab) {
            0 -> TodayScreen(
                dashboardViewModel = dashboardViewModel,
                language = language,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToSearch = onNavigateToSearch,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            1 -> ReportScreen(
                dashboardViewModel = dashboardViewModel,
                onNavigateToDayDetail = onNavigateToDayDetail,
                onNavigateToCategoryDetail = onNavigateToCategoryDetail,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO, name = "Light")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark")
@Composable
private fun DashboardScreenPreview() {
    DragSpendTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = true,
                        onClick = {},
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_today)) },
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Default.PieChart, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_report)) },
                    )
                }
            },
        ) {}
    }
}
