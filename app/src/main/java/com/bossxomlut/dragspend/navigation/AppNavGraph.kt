package com.bossxomlut.dragspend.navigation

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
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
import com.bossxomlut.dragspend.data.model.ProfileDto
import android.os.Build
import androidx.annotation.RequiresApi
import com.bossxomlut.dragspend.ui.screen.account.AccountDeletedScreen
import com.bossxomlut.dragspend.util.ProfileCache
import com.bossxomlut.dragspend.ui.screen.auth.ForgotPasswordScreen
import com.bossxomlut.dragspend.ui.screen.auth.LoginScreen
import com.bossxomlut.dragspend.ui.screen.auth.OTPVerificationScreen
import com.bossxomlut.dragspend.ui.screen.auth.RegisterScreen
import com.bossxomlut.dragspend.ui.screen.auth.ResetPasswordScreen
import com.bossxomlut.dragspend.ui.screen.dashboard.DashboardScreen
import com.bossxomlut.dragspend.ui.screen.dashboard.DashboardViewModel
import com.bossxomlut.dragspend.ui.screen.dashboard.report.CategoryDetailScreen
import com.bossxomlut.dragspend.ui.screen.dashboard.today.DayDetailScreen
import com.bossxomlut.dragspend.ui.screen.onboarding.LanguageScreen
import com.bossxomlut.dragspend.ui.screen.search.SearchScreen
import com.bossxomlut.dragspend.ui.screen.settings.SettingsScreen
import org.koin.androidx.compose.koinViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject
import android.os.SystemClock
import com.bossxomlut.dragspend.util.AppLog

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
    onReady: () -> Unit = {},
    modifier: Modifier = Modifier,
    profileCache: ProfileCache = koinInject(),
) {
    var startDestination by remember { mutableStateOf(StartDestination.CHECKING) }
    var selectedLanguage by remember { mutableStateOf("en") }

    // Always capture the latest lambda so LaunchedEffect(Unit) never needs to restart.
    val currentOnReady by rememberUpdatedState(onReady)

    LaunchedEffect(Unit) {
        val t0 = SystemClock.elapsedRealtime()
        AppLog.d(AppLog.Feature.PERF, "navGraph.start", "auth_check_begin")

        // Wait for Auth to finish loading the persisted session from storage
        supabase.auth.sessionStatus
            .first { it !is SessionStatus.Initializing }

        AppLog.d(AppLog.Feature.PERF, "navGraph.auth_ready", "${SystemClock.elapsedRealtime() - t0}ms")

        val session = supabase.auth.currentSessionOrNull()
        if (session == null) {
            AppLog.d(AppLog.Feature.PERF, "navGraph.no_session", "→ LOGIN, total=${SystemClock.elapsedRealtime() - t0}ms")
            startDestination = StartDestination.LOGIN
            currentOnReady()
            return@LaunchedEffect
        }

        val userId = session.user?.id ?: run {
            startDestination = StartDestination.LOGIN
            currentOnReady()
            return@LaunchedEffect
        }

        // Fast path: use the cached language to skip the network call on repeated launches.
        // The cache is only populated for fully-onboarded, non-deleted users.
        val cachedLanguage = profileCache.getLanguage(userId)
        if (cachedLanguage != null) {
            AppLog.success(AppLog.Feature.PERF, "navGraph.cache_hit", "→ DASHBOARD, total=${SystemClock.elapsedRealtime() - t0}ms")
            selectedLanguage = cachedLanguage
            startDestination = StartDestination.DASHBOARD
            currentOnReady()  // Dismiss splash immediately — before NavHost composes.
            return@LaunchedEffect
        }

        AppLog.d(AppLog.Feature.PERF, "navGraph.cache_miss", "fetching profile from network")

        // Slow path (first launch after install / sign-in / cache cleared): fetch from network.
        runCatching {
            val tFetch = SystemClock.elapsedRealtime()
            val profile = supabase.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<ProfileDto>()

            AppLog.d(AppLog.Feature.PERF, "navGraph.profile_fetched", "network=${SystemClock.elapsedRealtime() - tFetch}ms, total=${SystemClock.elapsedRealtime() - t0}ms")

            startDestination = when {
                profile?.deletedAt != null -> StartDestination.ACCOUNT_DELETED
                profile?.language == null -> StartDestination.ONBOARDING
                else -> StartDestination.DASHBOARD
            }
            selectedLanguage = profile?.language ?: "vi"

            // Persist language so future startups skip this network call.
            if (startDestination == StartDestination.DASHBOARD) {
                profileCache.saveLanguage(userId, selectedLanguage)
            }
        }.onFailure { e ->
            AppLog.error(AppLog.Feature.PERF, "navGraph.profile_fetch_failed", e, "→ LOGIN")
            startDestination = StartDestination.LOGIN
        }
        currentOnReady()
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
                onLoginNeedsOnboarding = {
                    navController.navigate(Route.Onboarding.route) {
                        popUpTo(Route.Login.route) { inclusive = true }
                    }
                },
                onNavigateToOTP = { email ->
                    navController.navigate(Route.OTPVerification.createRoute(email, "register"))
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
                    onNavigateToOTP = { email ->
                        navController.navigate(Route.OTPVerification.createRoute(email, "register")) {
                            popUpTo(Route.Register.route) { inclusive = true }
                        }
                    },
                    selectedLanguage = selectedLanguage,
                    onLanguageSelected = { selectedLanguage = it },
                )
            }

            composable(Route.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToOTP = { email ->
                        navController.navigate(Route.OTPVerification.createRoute(email, "forgot_password"))
                    },
                )
            }

            composable(
                route = Route.OTPVerification.route,
                arguments = listOf(
                    navArgument("email") { type = NavType.StringType },
                    navArgument("source") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val email = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("email") ?: "",
                    "UTF-8",
                )
                val source = backStackEntry.arguments?.getString("source") ?: "forgot_password"
                OTPVerificationScreen(
                    email = email,
                    source = source,
                    onNavigateBack = { navController.popBackStack() },
                    onVerified = {
                        if (source == "register") {
                            navController.navigate(Route.Onboarding.route) {
                                popUpTo(Route.Login.route) { inclusive = false }
                            }
                        } else {
                            navController.navigate(Route.ResetPassword.route)
                        }
                    },
                )
            }

            composable(Route.ResetPassword.route) {
                ResetPasswordScreen(
                    onNavigateToLogin = {
                        navController.navigate(Route.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }

            composable(Route.Onboarding.route) {
                LanguageScreen(
                    onOnboardingComplete = { language ->
                        val userId = supabase.auth.currentUserOrNull()?.id
                        if (userId != null) profileCache.saveLanguage(userId, language)
                        navController.navigate(Route.Dashboard.route) {
                            popUpTo(Route.Onboarding.route) { inclusive = true }
                        }
                    },
                )
            }

            composable(Route.Dashboard.route) {
                DashboardScreen(
                    onSignOut = {
                        profileCache.clear()
                        navController.navigate(Route.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToSettings = {
                        if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                            navController.navigate(Route.Settings.route)
                        }
                    },
                    onNavigateToDayDetail = { date ->
                        if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                            navController.navigate(Route.DayDetail.createRoute(date))
                        }
                    },
                    onNavigateToCategoryDetail = { yearMonth, categoryId, categoryName, categoryIcon ->
                        if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                            navController.navigate(
                                Route.CategoryDetail.createRoute(yearMonth, categoryId, categoryName, categoryIcon),
                            )
                        }
                    },
                    onNavigateToSearch = {
                        if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                            navController.navigate(Route.Search.route)
                        }
                    },
                )
            }

            composable(Route.Search.route) {
                SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Route.Settings.route) {
                SettingsScreen(
                    onDone = { navController.popBackStack() },
                    onSignOut = {
                        profileCache.clear()
                        navController.navigate(Route.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onAccountDeleted = {
                        navController.navigate(Route.AccountDeleted.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onLanguageSelected = { language ->
                        val userId = supabase.auth.currentUserOrNull()?.id
                        if (userId != null) profileCache.saveLanguage(userId, language)
                        selectedLanguage = language
                    },
                )
            }

            composable(Route.AccountDeleted.route) {
                AccountDeletedScreen(
                    onSignOut = {
                        profileCache.clear()
                        navController.navigate(Route.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }

            composable(
                route = Route.DayDetail.route,
                arguments = listOf(
                    navArgument("date") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getString("date") ?: ""
                val dashboardViewModel: DashboardViewModel = koinViewModel(
                    viewModelStoreOwner = navController.getBackStackEntry(Route.Dashboard.route),
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    DayDetailScreen(
                        date = date,
                        dashboardViewModel = dashboardViewModel,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
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
                val yearMonth = backStackEntry.arguments?.getString("yearMonth") ?: ""
                val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
                val categoryName = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("categoryName") ?: "",
                    "UTF-8",
                )
                val categoryIcon = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("categoryIcon") ?: "",
                    "UTF-8",
                )
                CategoryDetailScreen(
                    yearMonth = yearMonth,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    categoryIcon = categoryIcon,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
