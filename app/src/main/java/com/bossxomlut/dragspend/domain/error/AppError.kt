package com.bossxomlut.dragspend.domain.error

sealed class AppError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {

    /** Network I/O failures — no connectivity, DNS, timeouts. */
    class Network(cause: Throwable) : AppError(
        message = cause.message ?: "Network error",
        cause = cause,
    )

    /** No active session / JWT expired. */
    data object Unauthorized : AppError(message = "Session expired. Please log in again.")

    /** Requested resource doesn't exist. */
    class NotFound(message: String = "Resource not found") : AppError(message = message)

    /** Supabase / backend returned a business-rule error. */
    class Server(message: String) : AppError(message = message)

    /** Anything not covered by the above. */
    class Unknown(cause: Throwable) : AppError(
        message = cause.message ?: "An unexpected error occurred",
        cause = cause,
    )
}

/** Maps any [Throwable] to a structured [AppError]. Already-mapped errors pass through unchanged. */
fun Throwable.toAppError(): AppError {
    if (this is AppError) return this
    val raw = message?.lowercase() ?: ""
    return when {
        raw.contains("networkonmainthread") ||
            raw.contains("unknownhostexception") ||
            raw.contains("unable to resolve host") ||
            raw.contains("failed to connect") ||
            raw.contains("no address associated") ||
            raw.contains("sockettimeoutexception") ||
            raw.contains("timeout") ||
            raw.contains("timed out") -> AppError.Network(this)

        raw.contains("jwt expired") ||
            raw.contains("not authenticated") ||
            raw.contains("invalid jwt") ||
            raw.contains("session_not_found") -> AppError.Unauthorized

        else -> AppError.Unknown(this)
    }
}

/** Converts a [Result] failure to [AppError]. Success is passed through unchanged. */
fun <T> Result<T>.mapToAppError(): Result<T> = fold(
    onSuccess = { this },
    onFailure = { Result.failure(it.toAppError()) },
)

/** Returns a user-facing message suitable for display in a Snackbar or error UI. */
fun AppError.toFriendlyMessage(): String = when (this) {
    is AppError.Network -> "No internet connection. Please check your network."
    AppError.Unauthorized -> "Session expired. Please log in again."
    is AppError.NotFound -> "The requested resource was not found."
    is AppError.Server -> resolveFriendlyServerMessage(message ?: "")
    is AppError.Unknown -> resolveFriendlyServerMessage(cause?.message ?: message ?: "")
}

private fun resolveFriendlyServerMessage(raw: String): String {
    val lower = raw.lowercase()
    return when {
        lower.contains("invalid login credentials") ||
            lower.contains("invalid_credentials") ||
            lower.contains("wrong password") ||
            lower.contains("invalid email or password") ->
            "Incorrect email or password. Please try again."

        lower.contains("email not confirmed") ||
            lower.contains("email_not_confirmed") ->
            "Please check your inbox and confirm your email first."

        lower.contains("user already registered") ||
            lower.contains("already been registered") ||
            lower.contains("already exists") ->
            "This email is already registered. Try logging in instead."

        lower.contains("password should be at least") ||
            lower.contains("weak password") ->
            "Password is too weak. Use at least 6 characters."

        lower.contains("invalid email") ||
            lower.contains("unable to validate email") ->
            "Please enter a valid email address."

        lower.contains("rate limit") ||
            lower.contains("too many requests") ->
            "Too many attempts. Please wait a moment and try again."

        lower.contains("sockettimeoutexception") ||
            lower.contains("timeout") ||
            lower.contains("timed out") ->
            "Request timed out. Please try again."

        lower.contains("otp_expired") ||
            lower.contains("token has expired") ||
            lower.contains("otp expired") ->
            "The verification code has expired. Please request a new one."

        lower.contains("token") && lower.contains("invalid") ->
            "Invalid verification code. Please try again."

        else -> raw.ifBlank { "An unexpected error occurred. Please try again." }
    }
}
