package com.aozijx.passly.features.vault.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.internal.VaultDetailCoordinatorState
import com.aozijx.passly.features.vault.AddType

internal class DetailState {
    var addType by mutableStateOf(AddType.NONE)
        internal set
    var detailCoordinatorState by mutableStateOf(VaultDetailCoordinatorState())
        internal set
    var itemToDelete by mutableStateOf<VaultEntry?>(null)
        internal set
}