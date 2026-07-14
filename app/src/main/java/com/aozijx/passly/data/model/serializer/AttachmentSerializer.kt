package com.aozijx.passly.data.model.serializer

import com.aozijx.passly.data.model.payload.backup.AttachmentPayload

fun AttachmentPayload.toJsonString(): String =
    AppJson.encodeToString(AttachmentPayload.serializer(), this)

fun String.toAttachmentPayload(): AttachmentPayload =
    AppJson.decodeFromString(AttachmentPayload.serializer(), this)
