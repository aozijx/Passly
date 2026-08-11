package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.EntryAggregate

/**
 * 字段读取器。
 *
 * 根据 [FieldKey] 从 [EntryAggregate] 中提取原始数据值，
 * 供复制、Autofill 等场景使用。处理逻辑对所有条目类型通用。
 */
interface EntryFieldReader {
    fun getFieldValue(entry: EntryAggregate, key: FieldKey): String?
}
