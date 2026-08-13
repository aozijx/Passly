package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.EntryAggregate

/**
 * 条目校验器。
 *
 * 每个 [EntryType][com.aozijx.passly.domain.entry.model.EntryType] 可绑定独立的实现，
 * 未绑定的类型使用 [DefaultEntryValidator]。
 */
interface EntryValidator {
    /**
     * 校验必填字段。返回 null 表示通过，返回错误信息表示失败。
     */
    fun validateRequiredFields(entry: EntryAggregate): String?

    /**
     * 校验字段内容合法性。返回 null 表示通过，返回错误信息表示失败。
     */
    fun validateFieldContent(entry: EntryAggregate): String?
}
