package com.aozijx.passly.domain.failure

/**
 * 预注册错误代码。
 *
 * 格式：`[DOMAIN]_[ERROR_NAME]`，全大写 UPPER_SNAKE。
 */
object FailureCode {
    // ============================== 认证 ==============================
    const val AUTH_CREDENTIAL_INCORRECT = "AUTH_CREDENTIAL_INCORRECT"
    const val AUTH_SESSION_EXPIRED = "AUTH_SESSION_EXPIRED"
    const val AUTH_BIOMETRIC_UNAVAILABLE = "AUTH_BIOMETRIC_UNAVAILABLE"
    const val AUTH_LOCKED_OUT = "AUTH_LOCKED_OUT"
    const val AUTH_RECOVERY_INVALID = "AUTH_RECOVERY_INVALID"
    const val AUTH_MASTER_PASSWORD_WEAK = "AUTH_MASTER_PASSWORD_WEAK"

    // ============================== 备份 ==============================
    const val BACKUP_FILE_NOT_FOUND = "BACKUP_FILE_NOT_FOUND"
    const val BACKUP_FORMAT_UNSUPPORTED = "BACKUP_FORMAT_UNSUPPORTED"
    const val BACKUP_CORRUPTED = "BACKUP_CORRUPTED"
    const val BACKUP_ENCRYPTION_MISMATCH = "BACKUP_ENCRYPTION_MISMATCH"
    const val BACKUP_IO_ERROR = "BACKUP_IO_ERROR"
    const val BACKUP_IMPORT_CONFLICT = "BACKUP_IMPORT_CONFLICT"

    // ============================== 条目 ==============================
    const val ENTRY_NOT_FOUND = "ENTRY_NOT_FOUND"
    const val ENTRY_DUPLICATE = "ENTRY_DUPLICATE"
    const val ENTRY_VALIDATION_FAILED = "ENTRY_VALIDATION_FAILED"
    const val ENTRY_STORAGE_FAILED = "ENTRY_STORAGE_FAILED"
    const val ENTRY_VERSION_CONFLICT = "ENTRY_VERSION_CONFLICT"
}
