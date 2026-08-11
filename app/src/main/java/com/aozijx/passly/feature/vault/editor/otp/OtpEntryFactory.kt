package com.aozijx.passly.feature.vault.editor.otp

import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.model.WebsiteInfo
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.secret.OtpSecret
import com.aozijx.passly.feature.vault.model.OtpFormState

internal object OtpEntryFactory {

    fun create(
        state: OtpFormState,
        now: Long = System.currentTimeMillis()
    ): EntryAggregate = EntryAggregate(
        header = EntryHeader(
            id = EntryId(""),
            entryType = EntryType.OTP,
            version = EntryVersion.INITIAL,
            createdAt = now,
            updatedAt = now
        ),
        summary = EntrySummary(
            title = state.title.trim(),
            username = state.username.trim(),
            website = state.domain.trim()
                .takeIf(String::isNotEmpty)
                ?.let { WebsiteInfo(primaryUrl = it) }
        ),
        secret = EntrySecret(
            otp = OtpSecret(config = createConfig(state))
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
        accountName = state.username.trim().takeIf(String::isNotEmpty)
    )
}
