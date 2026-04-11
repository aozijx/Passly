package com.aozijx.passly.features.detail.page

import com.aozijx.passly.domain.model.core.VaultEntry

enum class DetailLaunchMode {
    VIEW,
    EDIT_FIELDS,
    EDIT_TOTP
}

data class DetailOpenRequest(
    val entry: VaultEntry,
    val launchMode: DetailLaunchMode = DetailLaunchMode.VIEW
)
