package com.aozijx.passly.core.error

// ============================================
// 文件说明：集中管理所有业务错误码（Error Code）
// 格式：统一使用大写 + 下划线，便于辨识
// ============================================

// ─── 认证相关 ──────────────────────────────
const val AUTH_FAILED = "AUTH_FAILED"
const val BIOMETRIC_UNAVAILABLE = "BIOMETRIC_UNAVAILABLE"
const val BIOMETRIC_NOT_ENROLLED = "BIOMETRIC_NOT_ENROLLED"
const val BIOMETRIC_LOCKED_OUT = "BIOMETRIC_LOCKED_OUT"
const val APP_PASSWORD_INCORRECT = "APP_PASSWORD_INCORRECT"
const val APP_LOCKED = "APP_LOCKED"

// ─── 数据库相关 ────────────────────────────
const val DATABASE_LOCKED = "DATABASE_LOCKED"
const val DATABASE_INIT_FAILED = "DATABASE_INIT_FAILED"

// ─── 备份相关 ──────────────────────────────
const val BACKUP_FAILED = "BACKUP_FAILED"

// ─── 网络与 IO ─────────────────────────────
const val NETWORK_ERROR = "NETWORK_ERROR"
const val FILE_IO_ERROR = "FILE_IO_ERROR"

// ─── 加密相关 ──────────────────────────────
const val CRYPTO_ERROR = "CRYPTO_ERROR"
const val KEY_STATE_ERROR = "KEY_STATE_ERROR"
const val CRYPTO_DATA_CORRUPTED = "CRYPTO_DATA_CORRUPTED"

// ─── 业务逻辑（Domain） ───────────────────
const val VALIDATION_ERROR = "VALIDATION_ERROR"
const val NOT_FOUND = "NOT_FOUND"
const val RATE_LIMITED = "RATE_LIMITED"
const val CONFLICT = "CONFLICT"
const val SESSION_MODE_RESTRICTED = "SESSION_MODE_RESTRICTED"

// ─── 兜底/未知 ─────────────────────────────
const val UNEXPECTED = "UNEXPECTED"
