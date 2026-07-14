package com.aozijx.passly.data.model.serializer

import com.aozijx.passly.data.model.payload.credential.CredentialPayload

fun CredentialPayload.toJsonString(): String =
    AppJson.encodeToString(CredentialPayload.serializer(), this)

fun String.toCredentialPayload(): CredentialPayload =
    AppJson.decodeFromString(CredentialPayload.serializer(), this)
