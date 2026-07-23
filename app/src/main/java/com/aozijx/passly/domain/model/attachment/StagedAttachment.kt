package com.aozijx.passly.domain.model.attachment

data class StagedAttachment(
    val fileName: String,
    val fileSize: Long,
    val mimeType: String?,
    val localPath: String
)
