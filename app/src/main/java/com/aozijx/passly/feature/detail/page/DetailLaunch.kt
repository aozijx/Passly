package com.aozijx.passly.feature.detail.page

import com.aozijx.passly.domain.model.entry.VaultEntry

enum class DetailLaunchMode {
    VIEW,
    EDIT_FIELDS,
    EDIT_TOTP
}

data class DetailOpenRequest(
    val entry: VaultEntry,
    val launchMode: DetailLaunchMode = DetailLaunchMode.VIEW
)