package com.bossxomlut.dragspend.util

import com.bossxomlut.dragspend.domain.error.AppError
import com.bossxomlut.dragspend.domain.error.toFriendlyMessage

/** Legacy convenience — delegates through [AppError.toFriendlyMessage] for consistent messages. */
fun Throwable.toFriendlyMessage(): String = (this as? AppError)?.toFriendlyMessage()
    ?: AppError.Unknown(this).toFriendlyMessage()
