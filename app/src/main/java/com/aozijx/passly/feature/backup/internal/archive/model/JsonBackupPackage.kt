package com.aozijx.passly.feature.backup.internal.archive.model

import kotlinx.serialization.Serializable

/**
 * 单文件明文 JSON 备份。
 *
 * [document] 保存结构化数据和资源元数据；[resourcesBase64] 保存资源原文。
 * 空资源集合不会被序列化。
 */
@Serializable
data class JsonBackupPackage(
    val document: BackupDocument,
    val resourcesBase64: Map<String, String> = emptyMap()
)
