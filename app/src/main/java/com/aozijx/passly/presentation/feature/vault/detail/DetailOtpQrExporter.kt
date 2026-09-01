package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.feature.vault.otp.OtpAuthUriCodec

internal class DetailOtpQrExporter(
    private val authorizationGate: AuthorizationGate,
    private val sensitiveFieldRepository: SensitiveFieldRepository,
    private val formatUri: (OtpConfig, String) -> String = OtpAuthUriCodec::format,
) {
    suspend fun export(entry: Entry): String? {
        val config = entry.secret.otp?.config ?: return null
        val result = authorizationGate.authorize(
            AuthorizationScope.SensitiveFields(
                entryId = entry.id,
                fieldKeys = setOf(SensitiveFieldKey.OTP_SECRET),
                action = SensitiveAccessAction.REVEAL,
            ),
        ) { permit ->
            val revealed = sensitiveFieldRepository.reveal(
                entryId = entry.id,
                key = SensitiveFieldKey.OTP_SECRET,
                permit = permit,
            ) ?: return@authorize null
            val secretChars = revealed.value.toCharArray()
            try {
                formatUri(
                    config.copy(secret = String(secretChars)),
                    entry.title,
                )
            } finally {
                secretChars.fill('\u0000')
                revealed.value.wipe()
            }
        }
        return (result as? AuthorizationResult.Allowed)?.value
    }
}
