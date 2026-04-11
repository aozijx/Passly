package com.aozijx.passly.features.vault.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.setValue
import com.aozijx.passly.core.designsystem.model.AddType
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.internal.VaultDetailCoordinatorState


internal class VaultDetailStateHolder {
    var addType by mutableStateOf(AddType.NONE)
    var detailCoordinatorState by mutableStateOf(VaultDetailCoordinatorState())
    var itemToDelete by mutableStateOf<VaultEntry?>(null)
}
