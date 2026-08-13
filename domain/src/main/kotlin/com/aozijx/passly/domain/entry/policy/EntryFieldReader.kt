package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.Entry

/**
 * 字段读取器。
 *
 * 根据 [FieldKey] 从 [Entry] 中提取原始数据值，
 * 供复制、Autofill 等场景使用。处理逻辑对所有条目类型通用。
 */
interface EntryFieldReader {
    fun getFieldValue(entry: Entry, key: FieldKey): String?
}
