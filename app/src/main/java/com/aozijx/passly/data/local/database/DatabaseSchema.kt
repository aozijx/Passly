package com.aozijx.passly.data.local.database

object DatabaseSchema {

    const val VERSION = 1
    const val DATABASE_NAME = "passly.db"

    // 核心数据表
    const val TABLE_METADATA = "vault_metadata"
    const val TABLE_CREDENTIALS = "vault_credentials"

    // 功能表
    const val TABLE_HISTORY = "vault_historys"
    const val TABLE_ACTIVITY = "vault_activities"
    const val TABLE_ATTACHMENT = "vault_attachments"
    const val TABLE_LOOKUP_INDEX = "lookup_index"
}