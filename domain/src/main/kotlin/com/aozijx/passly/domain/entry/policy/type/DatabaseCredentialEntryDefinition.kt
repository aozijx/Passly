package com.aozijx.passly.domain.entry.policy.type

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition

internal object DatabaseCredentialEntryDefinition {
    val definition = EntryTypeDefinition(EntryType.DATABASE_CREDENTIAL, loginFields())
}
