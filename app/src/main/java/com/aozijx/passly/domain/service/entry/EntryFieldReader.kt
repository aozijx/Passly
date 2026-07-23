package com.aozijx.passly.domain.service.entry

import com.aozijx.passly.domain.model.entry.FieldKey
import com.aozijx.passly.domain.model.entry.VaultEntry

/**
 * 字段读取器。
 *
 * 根据 [FieldKey] 从 [VaultEntry] 中提取原始数据值，
 * 供复制、Autofill 等场景使用。处理逻辑对所有条目类型通用。
 */
interface EntryFieldReader {
    fun getFieldValue(entry: VaultEntry, key: FieldKey): String?
}
