package com.bossxomlut.dragspend.util

fun Throwable.toFriendlyMessage(): String {
    val raw = message?.lowercase() ?: ""
    return when {
        raw.contains("invalid login credentials") ||
            raw.contains("invalid_credentials") ||
            raw.contains("wrong password") ||
            raw.contains("invalid email or password") ->
            "Incorrect email or password. Please try again."

        raw.contains("email not confirmed") ||
            raw.contains("email_not_confirmed") ->
            "Please check your inbox and confirm your email first."

        raw.contains("user already registered") ||
            raw.contains("already been registered") ||
            raw.contains("already exists") ->
            "This email is already registered. Try logging in instead."

        raw.contains("password should be at least") ||
            raw.contains("weak password") ->
            "Password is too weak. Use at least 6 characters."

        raw.contains("invalid email") ||
            raw.contains("unable to validate email") ->
            "Please enter a valid email address."

        raw.contains("rate limit") ||
            raw.contains("too many requests") ->
            "Too many attempts. Please wait a moment and try again."

        raw.contains("networkonmainthread") ||
            raw.contains("unknownhostexception") ||
            raw.contains("unable to resolve host") ||
            raw.contains("failed to connect") ||
            raw.contains("no address associated") ->
            "No internet connection. Please check your network."

        raw.contains("sockettimeoutexception") ||
            raw.contains("timeout") ||
            raw.contains("timed out") ->
            "Request timed out. Please try again."

        raw.contains("jwt expired") ||
            raw.contains("session expired") ||
            raw.contains("refresh_token_not_found") ->
            "Your session has expired. Please log in again."

        raw.contains("permission denied") ||
            raw.contains("row-level security") ||
            raw.contains("violates row") ||
            raw.contains("42501") ->
            "You don't have permission to perform this action."

        raw.contains("duplicate key") ||
            raw.contains("23505") ->
            "This item already exists."

        raw.contains("foreign key") ||
            raw.contains("23503") ->
            "Cannot complete — related data is missing."

        raw.contains("500") ||
            raw.contains("internal server error") ->
            "Server error. Please try again in a moment."

        raw.contains("503") ||
            raw.contains("service unavailable") ->
            "Service is temporarily unavailable. Please try later."

        else -> "Something went wrong. Please try again."
    }
}
