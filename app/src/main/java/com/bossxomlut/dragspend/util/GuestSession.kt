package com.bossxomlut.dragspend.util

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

/**
 * Quản lý session cho người dùng chưa đăng nhập (guest).
 * Tạo và lưu trữ một UUID ổn định làm userId cho dữ liệu local.
 */
class GuestSession(context: Context) {

    private val prefs = context.getSharedPreferences("guest_session_v1", Context.MODE_PRIVATE)

    /** Trả về guestId hiện tại, tạo mới nếu chưa có. */
    fun getOrCreateGuestId(): String {
        return prefs.getString(KEY_GUEST_ID, null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit { putString(KEY_GUEST_ID, newId) }
            newId
        }
    }

    /** Ngôn ngữ đã chọn cho guest user. */
    fun getLanguage(): String? = prefs.getString(KEY_LANGUAGE, null)

    fun saveLanguage(language: String) {
        prefs.edit { putString(KEY_LANGUAGE, language) }
    }

    companion object {
        private const val KEY_GUEST_ID = "guest_id"
        private const val KEY_LANGUAGE = "language"
    }
}
