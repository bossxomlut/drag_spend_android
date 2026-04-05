package com.bossxomlut.dragspend.domain.usecase.backup

import com.bossxomlut.dragspend.data.local.BackupManager
import com.bossxomlut.dragspend.data.local.SyncManager
import com.bossxomlut.dragspend.domain.repository.SessionRepository
import com.bossxomlut.dragspend.util.AppLog
import com.bossxomlut.dragspend.util.GuestSession

/**
 * Orchestrate sao lưu dữ liệu local lên Supabase và khôi phục từ Supabase.
 *
 * ### Lifecycle migrations:
 * 1. **Khi login** (`migrateRoomOnLogin`): Room guestId → supabaseId (không cần mạng).
 * 2. **Khi logout** (`migrateRoomOnSignOut`): Room supabaseId → guestId (giữ dữ liệu sau logout).
 * 3. **Backup thủ công** (`backupToCloud`): push dữ liệu lên Supabase.
 *
 * ### Restore:
 * - `restoreFromCloud` : kéo toàn bộ dữ liệu từ Supabase về Room.
 */
class BackupDataUseCase(
    private val sessionRepository: SessionRepository,
    private val guestSession: GuestSession,
    private val backupManager: BackupManager,
    private val syncManager: SyncManager,
) {

    /**
     * Gọi TRƯỚC khi thực sự sign out — migrate dữ liệu Room từ supabaseId → guestId.
     * Nhờ đó sau khi logout, người dùng vẫn thấy đầy đủ dữ liệu dưới guest mode.
     * Cần gọi khi `getCurrentUserId()` còn trả về supabaseId (trước `sessionRepository.signOut()`).
     */
    suspend fun migrateRoomOnSignOut() {
        val supabaseId = sessionRepository.getCurrentUserId() ?: return
        val guestId = guestSession.getOrCreateGuestId()
        if (guestId == supabaseId) return
        AppLog.d(AppLog.Feature.PERF, "BackupDataUseCase.migrateRoomOnSignOut", "supabaseId=${supabaseId.take(8)}")
        backupManager.migrateRoomUserId(fromUserId = supabaseId, toUserId = guestId)
    }

    /**
     * Gọi ngay sau khi login thành công để migrate dữ liệu guest sang Supabase userId.
     * Bước 1 (Room migration) chạy synchronous trước khi navigate — tránh màn hình trống.
     * Bước 2 (Supabase push) nên được gọi fire-and-forget sau khi navigate.
     */
    suspend fun migrateRoomOnLogin() {
        val supabaseId = sessionRepository.getCurrentUserId() ?: return
        val guestId = guestSession.getOrCreateGuestId()
        if (guestId == supabaseId) return
        AppLog.d(AppLog.Feature.PERF, "BackupDataUseCase.migrateRoomOnLogin", "guestId=${guestId.take(8)}")
        backupManager.migrateRoomUserId(fromUserId = guestId, toUserId = supabaseId)
    }

    /**
     * Push dữ liệu local của người dùng đã xác thực lên Supabase.
     * Cần kết nối mạng.
     */
    suspend fun backupToCloud(): Result<BackupManager.BackupResult> {
        val supabaseId = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not authenticated"))
        return backupManager.pushAll(supabaseId)
    }

    /**
     * Kéo toàn bộ dữ liệu từ Supabase về Room (ghi đè dữ liệu local).
     * Cần kết nối mạng.
     */
    suspend fun restoreFromCloud(): Result<Unit> {
        val supabaseId = sessionRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("Not authenticated"))
        return runCatching { syncManager.pullAll(supabaseId) }
    }
}
