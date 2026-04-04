package com.bossxomlut.dragspend.util

import android.util.Log
import com.bossxomlut.dragspend.BuildConfig

/**
 * Structured logger for DragSpend.
 *
 * Active only in DEBUG and PROFILE builds (ENABLE_LOGGING = true).
 *
 * Logcat format:
 *   D  DS:FEATURE  ► action | detail          ← start / info
 *   I  DS:FEATURE  ✓ action | detail          ← success
 *   E  DS:FEATURE  ✗ action | message         ← failure
 *   W  DS:FEATURE  ⚠ action | detail          ← warning
 *
 * Filter in Logcat: tag:DS:
 */
object AppLog {

    private val isEnabled = BuildConfig.ENABLE_LOGGING

    enum class Feature(val tag: String) {
        APP("DS:APP"),
        AUTH("DS:AUTH"),
        TRANSACTION("DS:TRANSACTION"),
        CARD("DS:CARD"),
        CATEGORY("DS:CATEGORY"),
        PROFILE("DS:PROFILE"),
        DASHBOARD("DS:DASHBOARD"),
        REPORT("DS:REPORT"),
        SETTINGS("DS:SETTINGS"),
        ONBOARDING("DS:ONBOARDING"),
        PERF("DS:PERF"),
    }

    /** General info / action start. */
    fun d(feature: Feature, action: String, detail: String = "") {
        if (!isEnabled) return
        Log.d(feature.tag, "► $action | $detail")
    }

    /** Action completed successfully. */
    fun success(feature: Feature, action: String, detail: String = "") {
        if (!isEnabled) return
        Log.i(feature.tag, "✓ $action | $detail")
    }

    /** Action failed with an exception. */
    fun error(feature: Feature, action: String, throwable: Throwable? = null, detail: String = "") {
        if (!isEnabled) return
        val message = throwable?.message?.takeIf { it.isNotBlank() } ?: detail
        Log.e(feature.tag, "✗ $action | $message", throwable)
    }

    /** Warning — unexpected but non-fatal state. */
    fun w(feature: Feature, action: String, detail: String = "") {
        if (!isEnabled) return
        Log.w(feature.tag, "⚠ $action | $detail")
    }
}

/**
 * Chains structured logging onto a [Result].
 * Logs success with [successDetail] or failure with the throwable message.
 */
fun <T> Result<T>.logResult(
    feature: AppLog.Feature,
    action: String,
    successDetail: (T) -> String = { "" },
): Result<T> = onSuccess { value ->
    AppLog.success(feature, action, successDetail(value))
}.onFailure { e ->
    AppLog.error(feature, action, e)
}
