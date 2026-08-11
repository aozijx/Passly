package com.aozijx.passly.domain.entry.model.attachment

data class StagedAttachment(
    val fileName: String,
    val fileSize: Long,
    val mimeType: String?,
    val localPath: String
)
