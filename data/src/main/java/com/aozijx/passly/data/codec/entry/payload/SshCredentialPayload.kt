package com.aozijx.passly.data.codec.entry.payload

import kotlinx.serialization.Serializable

@Serializable
data class SshCredentialPayload(
    val privateKey: String? = null,
    val publicKey: String? = null,
    val passphrase: String? = null,
)
