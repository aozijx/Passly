package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.EntryAggregate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 默认条目校验器，用于未绑定自定义 [EntryValidator] 的条目类型。
 *
 * 所有校验均通过（返回 null），不做任何额外的必填字段或内容检查。
 */
@Singleton
class DefaultEntryValidator @Inject constructor() : EntryValidator {
    override fun validateRequiredFields(entry: EntryAggregate): String? = null
    override fun validateFieldContent(entry: EntryAggregate): String? = null
}
