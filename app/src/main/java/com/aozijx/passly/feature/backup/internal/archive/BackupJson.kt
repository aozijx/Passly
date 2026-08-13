package com.aozijx.passly.feature.backup.internal.archive

import kotlinx.serialization.json.Json

/**
 * 备份专用 JSON 序列化器。
 * 不输出 null 和默认值，以减小备份文件体积。
 */
internal val BackupJson = Json {
    explicitNulls = false
    encodeDefaults = false
    prettyPrint = true
    // v1 writers never emit undefined fields; readers accept additive fields
    // so newer exporters can extend metadata without breaking old imports.
    ignoreUnknownKeys = true
    coerceInputValues = false
}
