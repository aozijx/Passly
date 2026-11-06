package com.aozijx.passly.feature.vault.presentation.editor

import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.entry.EntryEditorSeed

internal fun EntryEditorSeed.Editable.matchesCurrentAccess(
    expectedType: EntryType,
    access: SecureSessionAccessState,
): Boolean =
    entry.type == expectedType &&
        access.hasFullSecureSessionAccess() &&
        access.authenticationState.value === authentication
