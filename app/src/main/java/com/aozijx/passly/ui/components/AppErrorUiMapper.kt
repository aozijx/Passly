package com.aozijx.passly.ui.components

import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.ErrorLayer

fun AppError.toUiMessage(defaultMessage: String = "操作失败，请稍后重试"): String {
    return when (this) {
        is AppError.AuthFailed -> message.ifBlank { "身份验证失败，请重试" }
        is AppError.DatabaseLocked -> "数据库已锁定，请先解锁"
        is AppError.DatabaseInitFailed -> message.ifBlank { "数据库初始化失败" }
        is AppError.BackupFailed -> message.ifBlank { "备份操作失败" }
        is AppError.Unexpected -> message.ifBlank { defaultMessage }
    }
}

fun Throwable.toUiMessage(defaultMessage: String = "操作失败，请稍后重试"): String {
    return if (this is AppError) {
        this.toUiMessage(defaultMessage)
    } else {
        AppError.fromThrowable(this, layer = ErrorLayer.UI).toUiMessage(defaultMessage)
    }
}