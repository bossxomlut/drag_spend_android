package com.bossxomlut.dragspend.navigation

sealed class Route(val route: String) {
    data object Login : Route("login")
    data object Register : Route("register")
    data object ForgotPassword : Route("forgot_password")
    data object Onboarding : Route("onboarding")
    data object Dashboard : Route("dashboard")
    data object Settings : Route("settings")
    data object AccountDeleted : Route("account_deleted")
    data object DayDetail : Route("day_detail/{date}") {
        fun createRoute(date: String) = "day_detail/$date"
    }

    data object CategoryDetail : Route("category_detail/{yearMonth}/{categoryId}/{categoryName}/{categoryIcon}") {
        fun createRoute(
            yearMonth: String,
            categoryId: String,
            categoryName: String,
            categoryIcon: String,
        ): String {
            val encodedName = java.net.URLEncoder.encode(categoryName, "UTF-8")
            val encodedIcon = java.net.URLEncoder.encode(categoryIcon, "UTF-8")
            return "category_detail/$yearMonth/$categoryId/$encodedName/$encodedIcon"
        }
    }

    data object Search : Route("search")
}
