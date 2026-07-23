package com.aozijx.passly.domain.service.entry

import com.aozijx.passly.domain.model.entry.VaultEntry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 默认条目默认值提供者。
 *
 * 不对条目做任何修改，直接返回传入的 [VaultEntry]。
 */
@Singleton
class DefaultEntryDefaultsProvider @Inject constructor() : EntryDefaultsProvider {
    override fun initializeDefaults(entry: VaultEntry): VaultEntry = entry
}
