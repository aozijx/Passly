package com.aozijx.passly.presentation.feature.common.error

import com.aozijx.passly.core.error.mapping.fromThrowable
import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.model.BACKUP_FAILED
import com.aozijx.passly.core.error.model.CRYPTO_DATA_CORRUPTED
import com.aozijx.passly.core.error.model.CRYPTO_ERROR
import com.aozijx.passly.core.error.model.DATABASE_INIT_FAILED
import com.aozijx.passly.core.error.model.DATABASE_LOCKED
import com.aozijx.passly.core.error.model.FILE_IO_ERROR
import com.aozijx.passly.core.error.model.NETWORK_ERROR
import com.aozijx.passly.core.error.model.NOT_FOUND
import com.aozijx.passly.core.error.model.UNEXPECTED
import com.aozijx.passly.core.error.model.VALIDATION_ERROR

/**
 * Maps a stable application error to copy suitable for inline presentation.
 *
 * Global notices have their own mapping and routing boundary; this mapper is only for UI state,
 * snackbar, dialog, and toast text owned by a feature.
 */
fun AppError.toUiMessage(defaultMessage: String = "操作失败，请稍后重试"): String =
    when (code) {
        DATABASE_LOCKED -> "数据库已锁定，请先解锁"
        DATABASE_INIT_FAILED -> "数据库初始化失败"
        BACKUP_FAILED -> "备份操作失败"
        NETWORK_ERROR -> "网络异常，请检查网络连接"
        FILE_IO_ERROR -> "文件操作失败"
        CRYPTO_ERROR -> "加解密操作失败"
        CRYPTO_DATA_CORRUPTED -> "加密数据损坏"
        VALIDATION_ERROR -> "输入数据无效"
        NOT_FOUND -> "未找到请求的资源"
        UNEXPECTED -> defaultMessage
        else -> defaultMessage
    }

fun Throwable.toUiMessage(defaultMessage: String = "操作失败，请稍后重试"): String =
    if (this is AppError) {
        toUiMessage(defaultMessage)
    } else {
        AppError.fromThrowable(this).toUiMessage(defaultMessage)
    }
