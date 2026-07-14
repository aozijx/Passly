package com.aozijx.passly.data.model.payload.backup

/**
 * 备份格式版本管理。
 *
 * 项目唯一版本引用点。未来 Migration 通过此版本号兼容旧备份。
 */
object BackupSchema {
    const val VERSION = 1
}
