package com.aozijx.passly.feature.vault.internal

import com.aozijx.passly.feature.detail.page.DetailOpenRequest

data class VaultDetailCoordinatorState(
    val request: DetailOpenRequest? = null,
    val isIconPickerVisible: Boolean = false
)