package com.bossxomlut.dragspend.util

import com.bossxomlut.dragspend.BuildConfig
import com.bossxomlut.dragspend.util.reporter.LogReporter
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Structured logger for DragSpend.
 *
 * Dispatches log events to all registered [LogReporter] implementations.
 * Call [install] once in [com.bossxomlut.dragspend.DragSpendApp.onCreate]
 * after Koin is started.
 *
 * Logcat format (via [com.bossxomlut.dragspend.util.reporter.LogcatReporter]):
 *   D  DS:FEATURE  ► action | detail          ← start / info
 *   I  DS:FEATURE  ✓ action | detail          ← success
 *   E  DS:FEATURE  ✗ action | message         ← failure
 *   W  DS:FEATURE  ⚠ action | detail          ← warning
 *
 * Filter in Logcat: tag:DS:
 */
object AppLog {

    private val isEnabled = BuildConfig.ENABLE_LOGGING

    /**
     * Thread-safe list — safe to read from any thread after [install] completes.
     */
    private val reporters = CopyOnWriteArrayList<LogReporter>()

    /**
     * Register one or more [LogReporter] implementations.
     * Must be called once before any log method is invoked.
     */
    fun install(vararg reporter: LogReporter) {
        reporters.addAll(reporter.toList())
    }

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
        reporters.forEach { it.onDebug(feature, action, detail) }
    }

    /** Action completed successfully. */
    fun success(feature: Feature, action: String, detail: String = "") {
        if (!isEnabled) return
        reporters.forEach { it.onSuccess(feature, action, detail) }
    }

    /**
     * Action failed with an exception.
     *
     * Always dispatches to all reporters regardless of [isEnabled] so that
     * Crashlytics receives non-fatal errors in release builds.
     * Each reporter decides independently whether to act (e.g. [LogcatReporter]
     * is a no-op in release).
     */
    fun error(feature: Feature, action: String, throwable: Throwable? = null, detail: String = "") {
        reporters.forEach { it.onError(feature, action, throwable, detail) }
    }

    /** Warning — unexpected but non-fatal state. */
    fun w(feature: Feature, action: String, detail: String = "") {
        if (!isEnabled) return
        reporters.forEach { it.onWarning(feature, action, detail) }
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
