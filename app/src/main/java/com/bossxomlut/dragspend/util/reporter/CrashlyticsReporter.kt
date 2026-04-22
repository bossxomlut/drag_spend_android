package com.bossxomlut.dragspend.util.reporter

import com.bossxomlut.dragspend.util.AppLog
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Routes log events to Firebase Crashlytics.
 *
 * - [onDebug], [onSuccess], [onWarning] write breadcrumb log lines visible inside a crash report.
 * - [onError] records a non-fatal exception (or a log line when no [Throwable] is available).
 *   This captures errors that don't crash the app but still indicate problems.
 *
 * Collection is fully controlled by the `firebase_crashlytics_collection_enabled`
 * manifest placeholder — no data is sent in debug builds.
 */
class CrashlyticsReporter : LogReporter {

    private val crashlytics: FirebaseCrashlytics? by lazy {
        try {
            FirebaseCrashlytics.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    /** True nếu Crashlytics component đã đăng ký thành công với FirebaseApp. */
    val isAvailable: Boolean get() = crashlytics != null

    override fun onDebug(feature: AppLog.Feature, action: String, detail: String) {
        crashlytics?.log("${feature.tag} ► $action | $detail")
    }

    override fun onSuccess(feature: AppLog.Feature, action: String, detail: String) {
        crashlytics?.log("${feature.tag} ✓ $action | $detail")
    }

    override fun onError(feature: AppLog.Feature, action: String, throwable: Throwable?, detail: String) {
        crashlytics?.setCustomKey("feature", feature.tag)
        crashlytics?.setCustomKey("action", action)
        if (throwable != null) {
            crashlytics?.recordException(throwable)
        } else {
            crashlytics?.log("${feature.tag} ✗ $action | $detail")
        }
    }

    override fun onWarning(feature: AppLog.Feature, action: String, detail: String) {
        crashlytics?.log("${feature.tag} ⚠ $action | $detail")
    }

    /** Call on login success / logout to correlate crashes with a specific user. */
    fun setUserId(id: String) = crashlytics?.setUserId(id)

    /** Call on logout to clear the user association. */
    fun clearUserId() = crashlytics?.setUserId("")
}
