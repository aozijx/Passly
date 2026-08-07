package com.aozijx.passly.core.error.presentation

import com.aozijx.passly.core.error.APP_LOCKED
import com.aozijx.passly.core.error.APP_PASSWORD_INCORRECT
import com.aozijx.passly.core.error.AUTH_FAILED
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.BACKUP_FAILED
import com.aozijx.passly.core.error.BIOMETRIC_LOCKED_OUT
import com.aozijx.passly.core.error.BIOMETRIC_NOT_ENROLLED
import com.aozijx.passly.core.error.BIOMETRIC_UNAVAILABLE
import com.aozijx.passly.core.error.BiometricLockedOut
import com.aozijx.passly.core.error.CRYPTO_DATA_CORRUPTED
import com.aozijx.passly.core.error.CRYPTO_ERROR
import com.aozijx.passly.core.error.DATABASE_INIT_FAILED
import com.aozijx.passly.core.error.DATABASE_LOCKED
import com.aozijx.passly.core.error.ErrorLayer
import com.aozijx.passly.core.error.FILE_IO_ERROR
import com.aozijx.passly.core.error.KEY_STATE_ERROR
import com.aozijx.passly.core.error.NETWORK_ERROR
import com.aozijx.passly.core.error.NOT_FOUND
import com.aozijx.passly.core.error.RATE_LIMITED
import com.aozijx.passly.core.error.UNEXPECTED
import com.aozijx.passly.core.error.VALIDATION_ERROR
import com.aozijx.passly.core.error.fromThrowable

/**
 * 将 AppError 映射为用户可读的 UI 消息。
 * 基于 [code] 匹配，UI 层无需依赖具体错误子类所在的物理子包。
 * 新增错误类型时只需在 ErrorCodes.kt 加常量，在此加分支处理。
 */
fun AppError.toUiMessage(defaultMessage: String = "操作失败，请稍后重试"): String {
    return when (code) {
        // ── 认证相关 ──
        AUTH_FAILED -> message.ifBlank { "认证失败，请重试" }
        BIOMETRIC_UNAVAILABLE -> message.ifBlank { "设备不支持生物识别" }
        BIOMETRIC_NOT_ENROLLED -> message.ifBlank { "请先在系统设置中录入生物识别信息" }
        BIOMETRIC_LOCKED_OUT -> {
            val sec = (this as? BiometricLockedOut)?.lockoutDurationMs?.div(1000) ?: 30
            "生物识别已锁定，请等待 $sec 秒后重试"
        }

        APP_PASSWORD_INCORRECT -> message.ifBlank { "密码不正确" }
        APP_LOCKED -> message.ifBlank { "请先解锁应用" }

        // ── 数据库相关 ──
        DATABASE_LOCKED -> "数据库已锁定，请先解锁"
        DATABASE_INIT_FAILED -> message.ifBlank { "数据库初始化失败" }

        // ── 备份相关 ──
        BACKUP_FAILED -> message.ifBlank { "备份操作失败" }

        // ── 网络 ──
        NETWORK_ERROR -> message.ifBlank { "网络异常，请检查网络连接" }

        // ── 文件 ──
        FILE_IO_ERROR -> message.ifBlank { "文件操作失败" }

        // ── 加密相关 ──
        CRYPTO_ERROR -> message.ifBlank { "加解密操作失败" }
        KEY_STATE_ERROR -> message.ifBlank { "密钥状态异常，请重试" }
        CRYPTO_DATA_CORRUPTED -> message.ifBlank { "加密数据损坏" }

        // ── 业务逻辑 ──
        VALIDATION_ERROR -> message.ifBlank { "输入数据无效" }
        NOT_FOUND -> message.ifBlank { "未找到请求的资源" }
        RATE_LIMITED -> message.ifBlank { "操作过于频繁，请稍后重试" }

        // ── 兜底 ──
        UNEXPECTED -> message.ifBlank { defaultMessage }

        else -> message.ifBlank { defaultMessage }
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
        AppError.fromThrowable(this, layer = ErrorLayer.UI).toUiMessage(defaultMessage)
    }
}
