package com.bossxomlut.dragspend.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Centralized connectivity state holder that provides a simple API to check
 * if the device is online or offline. This is used to:
 * 1. Show offline banner in the UI
 * 2. Guard Supabase calls to prevent unnecessary network errors
 */
class ConnectivityState(
    private val connectivityMonitor: ConnectivityMonitor,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isOnline = MutableStateFlow(connectivityMonitor.isConnected())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    val isOffline: Boolean
        get() = !_isOnline.value

    init {
        scope.launch {
            connectivityMonitor.observeConnectivity().collect { connected ->
                _isOnline.value = connected
            }
        }
    }

    /**
     * Returns true if the device currently has internet connectivity.
     */
    fun isConnected(): Boolean = _isOnline.value

    /**
     * Executes the given block only if the device is online.
     * Returns null if offline.
     */
    suspend fun <T> whenOnline(block: suspend () -> T): T? {
        return if (isConnected()) {
            block()
        } else {
            null
        }
    }

    /**
     * Executes the given block only if the device is online.
     * Returns the fallback value if offline.
     */
    suspend fun <T> whenOnlineOrElse(fallback: T, block: suspend () -> T): T {
        return if (isConnected()) {
            block()
        } else {
            fallback
        }
    }
}
