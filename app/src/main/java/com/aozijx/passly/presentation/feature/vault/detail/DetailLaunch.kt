package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry

enum class DetailLaunchMode {
    VIEW,
    EDIT_FIELDS,
    EDIT_TOTP
}

data class DetailOpenRequest(
    val entry: Entry,
    val launchMode: DetailLaunchMode = DetailLaunchMode.VIEW
)