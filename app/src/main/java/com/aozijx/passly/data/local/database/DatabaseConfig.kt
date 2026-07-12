package com.aozijx.passly.data.local.database

object DatabaseConfig {
    const val DATABASE_NAME = "vault_database"

    // 核心数据表
    const val TABLE_METADATA = "vault_metadata"
    const val TABLE_CREDENTIALS = "vault_credentials"

    // 功能表
    const val TABLE_HISTORY = "vault_history"
    const val TABLE_ACTIVITY = "vault_activity"
    const val TABLE_ATTACHMENTS = "vault_attachments"
    const val TABLE_LOOKUP_INDEX = "lookup_index"

    // Bootstrap / 密钥管理
    const val TABLE_KEY_ENVELOPES = "key_envelopes"

    const val VERSION = 1
}