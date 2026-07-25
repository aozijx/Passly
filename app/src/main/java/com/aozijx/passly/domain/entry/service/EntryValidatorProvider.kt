package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.EntryType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通过 Hilt multibinding 注入的 [EntryValidator] 提供者。
 *
 * 替代原有 [com.aozijx.passly.domain.strategy.EntryTypeStrategyProvider]，
 * 由 Hilt 在编译期保证 [EntryType] 到校验器的映射完整性。
 * 未映射的类型回退到 [DefaultEntryValidator]。
 */
@Singleton
class EntryValidatorProvider @Inject constructor(
    private val validators: Map<@JvmSuppressWildcards EntryType, @JvmSuppressWildcards EntryValidator>,
    private val defaultValidator: DefaultEntryValidator
) {
    fun getValidator(entryType: EntryType): EntryValidator {
        return validators[entryType] ?: defaultValidator
    }
}
