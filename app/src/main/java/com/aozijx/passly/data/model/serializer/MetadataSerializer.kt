package com.aozijx.passly.data.model.serializer

import com.aozijx.passly.data.model.payload.metadata.MetadataPayload

fun MetadataPayload.toJsonString(): String =
    AppJson.encodeToString(MetadataPayload.serializer(), this)

fun String.toMetadataPayload(): MetadataPayload =
    AppJson.decodeFromString(MetadataPayload.serializer(), this)
