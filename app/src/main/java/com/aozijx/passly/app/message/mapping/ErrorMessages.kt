package com.aozijx.passly.app.message.mapping

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
 * 将 AppError 映射为用户可读的 UI 消息（内联显示，如 Snackbar、输入框错误）。
 * 基于 [code] 匹配，UI 层无需依赖具体错误子类所在的物理子包。
 * 新增错误类型时只需在 ErrorCodes.kt 加常量，在此加分支处理。
 *
 * 对于需要发布到消息中心的通知，使用 [AppErrorNoticeMapper.toNoticeCode]。
 */
fun AppError.toUiMessage(defaultMessage: String = "操作失败，请稍后重试"): String {
    return when (code) {
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
}

/**
 * 将 Throwable 转换为 UI 消息。
 * 如果是 AppError 则使用其映射，否则转换为 AppError 后再映射。
 */
fun Throwable.toUiMessage(defaultMessage: String = "操作失败，请稍后重试"): String {
    return if (this is AppError) {
        this.toUiMessage(defaultMessage)
    } else {
        AppError.fromThrowable(this).toUiMessage(defaultMessage)
    }
}