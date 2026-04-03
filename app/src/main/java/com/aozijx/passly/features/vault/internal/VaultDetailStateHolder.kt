package com.aozijx.passly.features.vault.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aozijx.passly.core.common.AddType
import com.aozijx.passly.domain.model.VaultEntry

internal class VaultDetailStateHolder {
    var addType by mutableStateOf(AddType.NONE)
    var detailItem by mutableStateOf<VaultEntry?>(null)
    var itemToDelete by mutableStateOf<VaultEntry?>(null)
    var showIconPicker by mutableStateOf(false)
    var shouldStartDetailInEditMode by mutableStateOf(false)
    var shouldStartTotpEdit by mutableStateOf(false)
    var prefilledUsername by mutableStateOf<String?>(null)
    var prefilledPassword by mutableStateOf<String?>(null)
    var prefilledTotpSecret by mutableStateOf<String?>(null)
}
