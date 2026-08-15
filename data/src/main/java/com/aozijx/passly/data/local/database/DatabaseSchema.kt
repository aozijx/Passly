package com.aozijx.passly.data.local.database

object DatabaseSchema {

    const val VERSION = 1
    const val DATABASE_NAME = "passly.db"

    // 核心数据表
    const val TABLE_ENTRIES = "entries"
    const val TABLE_SECRET_FIELDS = "entry_secret_fields"
    const val TABLE_ENTRY_LINKS = "entry_links"

    // 功能表
    const val TABLE_REVISIONS = "entry_revisions"
    const val TABLE_ACTIVITY = "entry_activities"
    const val TABLE_ATTACHMENT_RESOURCES = "attachment_resources"
    const val TABLE_ATTACHMENT_REFS = "attachment_refs"
    const val TABLE_REVISION_ATTACHMENT_REFS = "revision_attachment_refs"
    const val TABLE_ATTACHMENT_GC_QUEUE = "attachment_gc_queue"
    const val TABLE_SEARCH_TOKENS = "entry_search_tokens"
}
