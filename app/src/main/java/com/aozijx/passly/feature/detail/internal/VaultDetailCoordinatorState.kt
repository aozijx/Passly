package com.aozijx.passly.feature.detail.internal

import com.aozijx.passly.feature.detail.page.DetailOpenRequest

data class VaultDetailCoordinatorState(
    val request: DetailOpenRequest? = null,
    val isIconPickerVisible: Boolean = false
)
