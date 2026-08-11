package com.aozijx.passly.data.local.database

object DatabaseSchema {

    const val VERSION = 1
    const val DATABASE_NAME = "passly.db"

    // 核心数据表
    const val TABLE_ENTRIES = "entries"
    const val TABLE_SECRETS = "entry_secrets"
    const val TABLE_SENSITIVE_FIELDS = "entry_sensitive_fields"
    const val TABLE_ENTRY_LINKS = "entry_links"

    // 功能表
    const val TABLE_REVISIONS = "entry_revisions"
    const val TABLE_ACTIVITY = "entry_activities"
    const val TABLE_ATTACHMENT = "entry_attachments"
    const val TABLE_SEARCH_TOKENS = "entry_search_tokens"
    const val TABLE_DRAFTS = "entry_drafts"
}
