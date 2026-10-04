package com.aozijx.passly.feature.backup.internal.archive.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BackupResourceKind {
    @SerialName("icon")
    ICON,

    @SerialName("attachment")
    ATTACHMENT,
}
