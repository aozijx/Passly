package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.EntryAggregate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 默认条目默认值提供者。
 *
 * 不对条目做任何修改，直接返回传入的 [EntryAggregate]。
 */
@Singleton
class DefaultEntryDefaultsProvider @Inject constructor() : EntryDefaultsProvider {
    override fun initializeDefaults(entry: EntryAggregate): EntryAggregate = entry
}
