package com.bossxomlut.dragspend.util.reporter

import com.bossxomlut.dragspend.util.AppLog

/**
 * Strategy interface for log output destinations.
 *
 * Each implementation decides independently whether to act on each event.
 * Register implementations via [AppLog.install] at application startup.
 *
 * Default implementations are no-ops — override only the methods you need.
 */
interface LogReporter {
    fun onDebug(feature: AppLog.Feature, action: String, detail: String) {}
    fun onSuccess(feature: AppLog.Feature, action: String, detail: String) {}
    fun onError(feature: AppLog.Feature, action: String, throwable: Throwable?, detail: String) {}
    fun onWarning(feature: AppLog.Feature, action: String, detail: String) {}
}
