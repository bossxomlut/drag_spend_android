package com.bossxomlut.dragspend.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bossxomlut.dragspend.data.model.Profile
import com.bossxomlut.dragspend.ui.screen.account.AccountDeletedScreen
import com.bossxomlut.dragspend.ui.screen.auth.ForgotPasswordScreen
import com.bossxomlut.dragspend.ui.screen.auth.LoginScreen
import com.bossxomlut.dragspend.ui.screen.auth.RegisterScreen
import com.bossxomlut.dragspend.ui.screen.dashboard.DashboardScreen
import com.bossxomlut.dragspend.ui.screen.onboarding.LanguageScreen
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

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

    LaunchedEffect(Unit) {
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
            )
        }

        composable(Route.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
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
