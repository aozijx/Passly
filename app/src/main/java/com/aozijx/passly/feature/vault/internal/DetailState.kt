package com.aozijx.passly.feature.vault.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aozijx.passly.domain.model.lookup.EntryListItem
import com.aozijx.passly.feature.vault.model.AddType

internal class DetailState {
    var addType by mutableStateOf<AddType?>(null)
        internal set
    var detailCoordinatorState by mutableStateOf(VaultDetailCoordinatorState())
        internal set
    var itemToDelete by mutableStateOf<EntryListItem?>(null)
        internal set
}
