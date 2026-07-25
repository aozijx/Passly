package com.aozijx.passly.feature.common.error

import com.aozijx.passly.domain.auth.failure.AuthFailure
import com.aozijx.passly.domain.backup.failure.BackupFailure
import com.aozijx.passly.domain.entry.failure.EntryFailure
import com.aozijx.passly.domain.failure.AppFailure
import com.aozijx.passly.domain.failure.RecoveryAction

/**
 * Failure → UI 文案映射。
 *
 * 每个失败类型映射为用户可见的消息字符串。
 * 仅用于一次性的 UI 通知（Snackbar / Toast / Dialog），不应记录日志。
 */
object FailureUiMapper {

    fun mapMessage(failure: AppFailure, defaultMessage: String = ""): String = when (failure) {
        // ============================== 认证 ==============================
        is AuthFailure.CredentialIncorrect -> "认证未通过"
        is AuthFailure.SessionExpired -> "会话已过期，请重新认证"
        is AuthFailure.BiometricUnavailable -> "生物识别不可用，请检查系统设置"
        is AuthFailure.LockedOut -> "多次尝试失败，账户已锁定"
        is AuthFailure.RecoveryCodeInvalid -> "恢复码无效"
        is AuthFailure.MasterPasswordWeak -> "主密码强度不足"

        // ============================== 备份 ==============================
        is BackupFailure.FileNotFound -> "备份文件不存在"
        is BackupFailure.FormatUnsupported -> "不支持的备份格式"
        is BackupFailure.Corrupted -> "备份文件已损坏，无法恢复"
        is BackupFailure.EncryptionMismatch -> "解密失败，请检查密码或密钥"
        is BackupFailure.IoError -> "备份读写失败，请重试"
        is BackupFailure.ImportConflict -> "导入时遇到同名条目"

        // ============================== 条目 ==============================
        is EntryFailure.NotFound -> "条目不存在"
        is EntryFailure.Duplicate -> "条目已存在"
        is EntryFailure.ValidationFailed -> "输入校验未通过"
        is EntryFailure.StorageFailed -> "保存失败，请重试"
        is EntryFailure.VersionConflict -> "条目版本冲突，请刷新后重试"

        // ============================== 兜底 ==============================
        else -> defaultMessage.ifEmpty { "操作失败" }
    }

    fun isRecoverable(failure: AppFailure): Boolean = failure.recoveryAction != RecoveryAction.NONE
}
