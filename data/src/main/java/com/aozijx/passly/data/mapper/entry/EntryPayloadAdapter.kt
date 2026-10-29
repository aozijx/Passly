package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.credential.EntryCredentialKind

internal interface EntryPayloadAdapter {
    val kind: EntryCredentialKind

    fun encode(credential: EntryCredential): SecretPayload

    fun decode(payload: SecretPayload): EntryCredential
}
