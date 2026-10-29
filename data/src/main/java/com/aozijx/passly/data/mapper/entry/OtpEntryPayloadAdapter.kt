package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.codec.entry.payload.OtpConfigPayload
import com.aozijx.passly.data.codec.entry.payload.OtpCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.credential.EntryCredentialKind
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.otp.OtpConfig

internal class OtpEntryPayloadAdapter : EntryPayloadAdapter {
    override val kind: EntryCredentialKind = EntryCredentialKind.OTP

    override fun encode(credential: EntryCredential): SecretPayload {
        require(credential is OtpCredential) { "OTP adapter requires OtpCredential" }
        return SecretPayload(otp = OtpCredentialPayload(credential.config.toPayload()))
    }

    override fun decode(payload: SecretPayload): EntryCredential {
        val config = requireNotNull(payload.otp?.config) { "OTP payload is missing config" }
        return OtpCredential(config.toDomain())
    }

    private fun OtpConfig.toPayload() = OtpConfigPayload(
        type = type,
        secret = secret,
        algorithm = algorithm,
        digits = digits,
        periodSeconds = periodSeconds,
        counter = counter,
        encoding = encoding,
        issuer = issuer,
        accountName = accountName,
    )

    private fun OtpConfigPayload.toDomain() = OtpConfig(
        type = type,
        secret = secret,
        algorithm = algorithm,
        digits = digits,
        periodSeconds = periodSeconds,
        counter = counter,
        encoding = encoding,
        issuer = issuer,
        accountName = accountName,
    )
}
