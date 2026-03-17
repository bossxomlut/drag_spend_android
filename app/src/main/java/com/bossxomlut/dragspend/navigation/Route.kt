package com.bossxomlut.dragspend.navigation

sealed class Route(val route: String) {
    data object Login : Route("login")
    data object Register : Route("register")
    data object ForgotPassword : Route("forgot_password")
    data object Onboarding : Route("onboarding")
    data object Dashboard : Route("dashboard")
    data object Settings : Route("settings")
    data object AccountDeleted : Route("account_deleted")
}
