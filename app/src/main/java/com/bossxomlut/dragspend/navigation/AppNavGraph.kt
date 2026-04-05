package com.bossxomlut.dragspend.navigation

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.bossxomlut.dragspend.ui.screen.account.AccountDeletedScreen
import com.bossxomlut.dragspend.util.GuestSession
import com.bossxomlut.dragspend.util.ProfileCache
import com.bossxomlut.dragspend.ui.screen.auth.ForgotPasswordScreen
import com.bossxomlut.dragspend.ui.screen.auth.LoginScreen
import com.bossxomlut.dragspend.ui.screen.auth.OTPVerificationScreen
import com.bossxomlut.dragspend.ui.screen.auth.RegisterScreen
import com.bossxomlut.dragspend.ui.screen.auth.ResetPasswordScreen
import com.bossxomlut.dragspend.ui.screen.dashboard.DashboardScreen
import com.bossxomlut.dragspend.ui.screen.onboarding.LanguageScreen
import com.bossxomlut.dragspend.ui.screen.search.SearchScreen
import com.bossxomlut.dragspend.ui.screen.settings.SettingsScreen
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
    guestSession: GuestSession = koinInject(),
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
            // Guest mode: không bắt buộc đăng nhập
            val guestLanguage = guestSession.getLanguage()
            selectedLanguage = guestLanguage ?: "en"
            startDestination = if (guestLanguage == null) StartDestination.ONBOARDING else StartDestination.DASHBOARD
            AppLog.d(AppLog.Feature.PERF, "navGraph.guest_mode", "lang=$guestLanguage → $startDestination, total=${SystemClock.elapsedRealtime() - t0}ms")
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
        // CHECKING không bao giờ được render — fallback về Dashboard
        StartDestination.CHECKING -> Route.Dashboard.route
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
                        selectedLanguage = language
                        val userId = supabase.auth.currentUserOrNull()?.id
                        if (userId != null) {
                            profileCache.saveLanguage(userId, language)
                        } else {
                            guestSession.saveLanguage(language)
                        }
                        navController.navigate(Route.Dashboard.route) {
                            popUpTo(Route.Onboarding.route) { inclusive = true }
                        }
                    },
                )
            }

            composable(Route.Dashboard.route) {
                DashboardScreen(
                    language = selectedLanguage,
                    onSignOut = {
                        profileCache.clear()
                        // Sign out → về Dashboard dạng guest (không bắt login)
                        navController.navigate(Route.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Route.Settings.route)
                    },
                    onNavigateToSearch = {
                        navController.navigate(Route.Search.route)
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
                    onNavigateToLogin = {
                        navController.navigate(Route.Login.route)
                    },
                    onSignOut = {
                        profileCache.clear()
                        // Sign out → về Dashboard dạng guest
                        navController.navigate(Route.Dashboard.route) {
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
        }
    }
}
