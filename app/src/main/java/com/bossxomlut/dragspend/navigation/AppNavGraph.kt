package com.bossxomlut.dragspend.navigation

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration
import java.util.Locale
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bossxomlut.dragspend.data.model.Profile
import com.bossxomlut.dragspend.ui.screen.account.AccountDeletedScreen
import com.bossxomlut.dragspend.ui.screen.auth.ForgotPasswordScreen
import com.bossxomlut.dragspend.ui.screen.auth.LoginScreen
import com.bossxomlut.dragspend.ui.screen.auth.RegisterScreen
import com.bossxomlut.dragspend.ui.screen.dashboard.DashboardScreen
import com.bossxomlut.dragspend.ui.screen.dashboard.DashboardViewModel
import com.bossxomlut.dragspend.ui.screen.dashboard.today.DayDetailScreen
import com.bossxomlut.dragspend.ui.screen.dashboard.report.CategoryDetailScreen
import com.bossxomlut.dragspend.ui.screen.onboarding.LanguageScreen
import com.bossxomlut.dragspend.ui.screen.settings.SettingsScreen
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.first
import org.koin.androidx.compose.koinViewModel

enum class StartDestination {
    CHECKING,
    LOGIN,
    ONBOARDING,
    ACCOUNT_DELETED,
    DASHBOARD,
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    supabase: SupabaseClient,
    modifier: Modifier = Modifier,
) {
    var startDestination by remember { mutableStateOf(StartDestination.CHECKING) }
    var selectedLanguage by remember { mutableStateOf("en") }

    LaunchedEffect(Unit) {
        // Wait for Auth to finish loading the persisted session from storage
        supabase.auth.sessionStatus
            .first { it !is SessionStatus.Initializing }

        val session = supabase.auth.currentSessionOrNull()
        if (session == null) {
            startDestination = StartDestination.LOGIN
            return@LaunchedEffect
        }

        runCatching {
            val userId = session.user?.id ?: run {
                startDestination = StartDestination.LOGIN
                return@LaunchedEffect
            }
            val profile = supabase.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<Profile>()

            startDestination = when {
                profile?.deletedAt != null -> StartDestination.ACCOUNT_DELETED
                profile?.language == null -> StartDestination.ONBOARDING
                else -> StartDestination.DASHBOARD
            }
            selectedLanguage = profile?.language ?: "vi"
        }.onFailure {
            startDestination = StartDestination.LOGIN
        }
    }

    if (startDestination == StartDestination.CHECKING) return

    val start = when (startDestination) {
        StartDestination.LOGIN -> Route.Login.route
        StartDestination.ONBOARDING -> Route.Onboarding.route
        StartDestination.ACCOUNT_DELETED -> Route.AccountDeleted.route
        StartDestination.DASHBOARD -> Route.Dashboard.route
        StartDestination.CHECKING -> Route.Login.route
    }

    val context = LocalContext.current
    val activityResultRegistryOwner = LocalActivityResultRegistryOwner.current
    val localizedContext = remember(selectedLanguage) {
        val locale = Locale.forLanguageTag(selectedLanguage)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        *listOfNotNull(
            activityResultRegistryOwner?.let {
                LocalActivityResultRegistryOwner provides it
            },
        ).toTypedArray(),
    ) {
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = modifier,
        ) {
            composable(Route.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Route.Dashboard.route) {
                        popUpTo(Route.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Route.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(Route.ForgotPassword.route) },
                selectedLanguage = selectedLanguage,
                onLanguageSelected = { selectedLanguage = it },
            )
            }

            composable(Route.Register.route) {
                RegisterScreen(
                    onNavigateToLogin = { navController.popBackStack() },
                    selectedLanguage = selectedLanguage,
                    onLanguageSelected = { selectedLanguage = it },
                )
            }

            composable(Route.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Route.Onboarding.route) {
                LanguageScreen(
                    onOnboardingComplete = {
                        navController.navigate(Route.Dashboard.route) {
                            popUpTo(Route.Onboarding.route) { inclusive = true }
                        }
                    },
                )
            }

            composable(Route.Dashboard.route) {
                DashboardScreen(
                    onSignOut = {
                        navController.navigate(Route.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Route.Settings.route)
                    },
                    onNavigateToDayDetail = { date ->
                        navController.navigate(Route.DayDetail.createRoute(date))
                    },
                    onNavigateToCategoryDetail = { yearMonth, categoryId, categoryName, categoryIcon ->
                        navController.navigate(Route.CategoryDetail.createRoute(yearMonth, categoryId, categoryName, categoryIcon))
                    },
                )
            }

            composable(
                route = Route.DayDetail.route,
                arguments = listOf(navArgument("date") { type = NavType.StringType }),
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getString("date") ?: return@composable
                val dashboardEntry = remember(navController) {
                    navController.getBackStackEntry(Route.Dashboard.route)
                }
                val dashVm: DashboardViewModel = koinViewModel(viewModelStoreOwner = dashboardEntry)
                DayDetailScreen(
                    date = date,
                    dashboardViewModel = dashVm,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Route.CategoryDetail.route,
                arguments = listOf(
                    navArgument("yearMonth") { type = NavType.StringType },
                    navArgument("categoryId") { type = NavType.StringType },
                    navArgument("categoryName") { type = NavType.StringType },
                    navArgument("categoryIcon") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val yearMonth = backStackEntry.arguments?.getString("yearMonth") ?: return@composable
                val categoryId = backStackEntry.arguments?.getString("categoryId") ?: return@composable
                val categoryName = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("categoryName") ?: "", "UTF-8"
                )
                val categoryIcon = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("categoryIcon") ?: "📦", "UTF-8"
                )
                CategoryDetailScreen(
                    yearMonth = yearMonth,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    categoryIcon = categoryIcon,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Route.Settings.route) {
                SettingsScreen(
                    onDone = { navController.popBackStack() },
                    onSignOut = {
                        navController.navigate(Route.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onAccountDeleted = {
                        navController.navigate(Route.AccountDeleted.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onLanguageSelected = { selectedLanguage = it },
                )
            }

            composable(Route.AccountDeleted.route) {
                AccountDeletedScreen(
                    onSignOut = {
                        navController.navigate(Route.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
