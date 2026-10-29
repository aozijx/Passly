package com.aozijx.passly.domain.entry.policy.type

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition

internal object LoginEntryDefinition {
    val definition = EntryTypeDefinition(EntryType.LOGIN, loginFields())
}
