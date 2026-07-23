package com.aozijx.passly.domain.strategy

import com.aozijx.passly.domain.model.entry.EntryType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通过 Hilt multibinding 注入的 [EntryTypeStrategy] 提供者。
 *
 * 替代全局可变的 [EntryTypeStrategyFactory]，由 Hilt 在编译期保证
 * [EntryType] 到策略的 1:1 映射完整性。
 */
@Singleton
class EntryTypeStrategyProvider @Inject constructor(
    private val strategies: Map<@JvmSuppressWildcards EntryType, @JvmSuppressWildcards EntryTypeStrategy>
) {
    fun getStrategy(entryType: EntryType): EntryTypeStrategy {
        return strategies[entryType]
            ?: throw IllegalArgumentException("没有找到类型 $entryType 对应的策略")
    }
}
