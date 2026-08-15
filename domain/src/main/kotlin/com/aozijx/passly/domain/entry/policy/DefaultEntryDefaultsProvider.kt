package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.Entry

/**
 * 默认条目默认值提供者。
 *
 * 不对条目做任何修改，直接返回传入的 [Entry]。
 */
class DefaultEntryDefaultsProvider : EntryDefaultsProvider {
    override fun initializeDefaults(entry: Entry): Entry = entry
}
