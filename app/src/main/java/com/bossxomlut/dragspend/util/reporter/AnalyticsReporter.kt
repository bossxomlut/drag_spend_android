package com.bossxomlut.dragspend.util.reporter

import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.AppTracker
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

/**
 * Routes log errors to Firebase Analytics and exposes [AppTracker] for
 * ViewModels that need to track explicit user actions.
 *
 * As a [LogReporter] it automatically measures error rates per feature from
 * every [AppLog.error] call — no extra plumbing needed at call sites.
 *
 * As an [AppTracker] it provides [logScreen] and [logEvent] for granular
 * action tracking injected via Koin into ViewModels.
 *
 * Collection is controlled by the `firebase_analytics_collection_deactivated`
 * manifest placeholder — no data is sent in debug builds.
 */
class AnalyticsReporter(private val analytics: FirebaseAnalytics) : LogReporter, AppTracker {

    override fun onError(feature: AppLog.Feature, action: String, throwable: Throwable?, detail: String) {
        analytics.logEvent("app_error") {
            param("feature", feature.tag)
            param("action", action)
            param("error_type", throwable?.javaClass?.simpleName ?: "unknown")
        }
    }

    override fun logScreen(screen: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screen)
        }
    }

    override fun logEvent(event: String, params: Map<String, String>) {
        analytics.logEvent(event) {
            params.forEach { (key, value) -> param(key, value) }
        }
    }

    override fun setUserId(id: String?) {
        analytics.setUserId(id)
    }
}
