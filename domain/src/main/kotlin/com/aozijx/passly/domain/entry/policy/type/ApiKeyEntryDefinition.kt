package com.aozijx.passly.domain.entry.policy.type

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition

internal object ApiKeyEntryDefinition {
    val definition = EntryTypeDefinition(EntryType.API_KEY, loginFields())
}
