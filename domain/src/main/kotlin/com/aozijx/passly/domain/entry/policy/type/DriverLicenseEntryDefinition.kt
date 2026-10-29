package com.aozijx.passly.domain.entry.policy.type

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition

internal object DriverLicenseEntryDefinition {
    val definition = EntryTypeDefinition(EntryType.DRIVER_LICENSE, identityFields())
}
