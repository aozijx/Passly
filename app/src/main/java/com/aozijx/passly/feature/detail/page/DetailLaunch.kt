package com.aozijx.passly.feature.detail.page

import com.aozijx.passly.domain.entry.model.EntryAggregate

enum class DetailLaunchMode {
    VIEW,
    EDIT_FIELDS,
    EDIT_TOTP
}

data class DetailOpenRequest(
    val entry: EntryAggregate,
    val launchMode: DetailLaunchMode = DetailLaunchMode.VIEW
)