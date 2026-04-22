package com.bossxomlut.dragspend.util.reporter

import android.util.Log
import com.bossxomlut.dragspend.BuildConfig
import com.bossxomlut.dragspend.util.AppLog

/**
 * Routes log events to Android Logcat.
 *
 * Only active when [BuildConfig.ENABLE_LOGGING] is true (debug / profile builds).
 * In release builds this reporter is a no-op, so [AppLog.error] can safely
 * dispatch to all reporters without leaking sensitive stack traces to Logcat.
 */
class LogcatReporter : LogReporter {

    private val enabled = BuildConfig.ENABLE_LOGGING

    override fun onDebug(feature: AppLog.Feature, action: String, detail: String) {
        if (!enabled) return
        Log.d(feature.tag, "► $action | $detail")
    }

    override fun onSuccess(feature: AppLog.Feature, action: String, detail: String) {
        if (!enabled) return
        Log.i(feature.tag, "✓ $action | $detail")
    }

    override fun onError(feature: AppLog.Feature, action: String, throwable: Throwable?, detail: String) {
        if (!enabled) return
        val message = throwable?.message?.takeIf { it.isNotBlank() } ?: detail
        Log.e(feature.tag, "✗ $action | $message", throwable)
    }

    override fun onWarning(feature: AppLog.Feature, action: String, detail: String) {
        if (!enabled) return
        Log.w(feature.tag, "⚠ $action | $detail")
    }
}
