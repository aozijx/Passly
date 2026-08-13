package com.aozijx.passly.feature.backup.internal.archive.model

/**
 * 从数据库读取的完整可备份数据。
 * 包含所有条目的业务数据，以及资源（图标、附件）的二进制内容。
 */
data class BackupBundle(
    val document: BackupDocument,
    val resourceData: Map<String, ByteArray> = emptyMap()
)
