package com.aozijx.passly.domain.service.entry

import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry

/**
 * 条目类型策略。
 *
 * 每个 [EntryType] 对应一份不可变策略配置，决定：
 * - [supportsAutofill]：是否支持 Autofill
 * - [suggestedCategory]：建议分类
 * - [sensitiveFields]：敏感字段集合
 * - [extractSummary]：从条目中提取摘要文本
 */
interface EntryTypePolicy {
    fun supportsAutofill(type: EntryType): Boolean
    fun suggestedCategory(type: EntryType): String
    fun sensitiveFields(type: EntryType): Set<String>
    fun extractSummary(type: EntryType, entry: VaultEntry): String
}
