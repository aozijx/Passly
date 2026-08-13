package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.Entry

/**
 * 条目默认值提供者。
 *
 * 新建条目时填充默认值。当前所有类型均使用 no-op 实现，
 * 后续可针对特定类型扩展。
 */
interface EntryDefaultsProvider {
    fun initializeDefaults(entry: Entry): Entry
}
