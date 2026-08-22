package com.aozijx.passly.feature.vault.editor.otp

import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.feature.vault.model.OtpFormState
import com.github.f4b6a3.uuid.UuidCreator

internal object OtpEntryFactory {

    fun create(
        state: OtpFormState,
        now: Long = System.currentTimeMillis()
    ): Entry = Entry(
        identity = EntryIdentity(
            id = EntryId(UuidCreator.getTimeOrderedEpoch().toString()),
            type = EntryType.OTP,
            version = EntryVersion.INITIAL,
            timestamps = EntryTimestamps(now),
        ),
        profile = EntryProfile(
            title = state.title.trim(),
            username = "",
            associations = EntryAssociations(),
        ),
        secret = EntrySecret(
            credential = OtpCredential(config = createConfig(state))
        )
    )

    private fun createConfig(state: OtpFormState): OtpConfig = OtpConfig(
        type = state.type,
        secret = state.secret.trim(),
        digits = if (state.type == OtpType.STEAM) {
            5
        } else {
            state.digits.trim().toIntOrNull() ?: 6
        },
        periodSeconds = if (state.type == OtpType.HOTP) {
            null
        } else {
            state.period.trim().toIntOrNull() ?: 30
        },
        counter = if (state.type == OtpType.HOTP) {
            state.counter.trim().toLongOrNull() ?: 0L
        } else {
            null
        },
        algorithm = OtpHashAlgorithm.entries.firstOrNull {
            it.name.equals(state.algorithm, ignoreCase = true)
        } ?: OtpHashAlgorithm.SHA1,
        encoding = state.encoding,
        issuer = state.issuer.trim().takeIf(String::isNotEmpty),
        accountName = state.accountName.trim().takeIf(String::isNotEmpty)
    )
}
