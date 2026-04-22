package com.bossxomlut.dragspend.util

/**
 * Abstraction for user-action tracking (Analytics).
 *
 * ViewModels depend on this interface rather than on Firebase Analytics directly,
 * keeping the presentation layer decoupled from the analytics infrastructure.
 *
 * Implemented by [com.bossxomlut.dragspend.util.reporter.AnalyticsReporter].
 */
interface AppTracker {
    fun logScreen(screen: String)
    fun logEvent(event: String, params: Map<String, String> = emptyMap())
    fun setUserId(id: String?)
}
