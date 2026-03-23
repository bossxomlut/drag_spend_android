package com.bossxomlut.dragspend.data.local.entity

/**
 * Tracks the synchronization state of a local record with the remote server.
 */
enum class SyncStatus {
    /**
     * Record is in sync with the server.
     */
    SYNCED,

    /**
     * Record was created locally and needs to be pushed to the server.
     */
    PENDING_CREATE,

    /**
     * Record was modified locally and needs to be pushed to the server.
     */
    PENDING_UPDATE,

    /**
     * Record was deleted locally and the deletion needs to be pushed to the server.
     */
    PENDING_DELETE,
}
