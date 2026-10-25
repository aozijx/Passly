package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.ErrorCodes
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.LogFieldKeys
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.fields
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.policy.WifiQrFormatResult
import com.aozijx.passly.domain.entry.policy.WifiQrPayload
import com.aozijx.passly.domain.entry.policy.WifiQrPayloadFormatter
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.vault.otp.OtpAuthUriCodec
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class ExportEntryQrUseCase internal constructor(
    private val authorizationGate: AuthorizationGate,
    private val sensitiveFieldRepository: SensitiveFieldRepository,
    private val activityRecorder: ActivityRecorder,
    private val logger: Logger,
    private val formatOtpUri: (OtpConfig, String) -> String,
) {
    @Inject
    constructor(
        authorizationGate: AuthorizationGate,
        sensitiveFieldRepository: SensitiveFieldRepository,
        activityRecorder: ActivityRecorder,
        logger: Logger,
    ) : this(authorizationGate, sensitiveFieldRepository, activityRecorder, logger, OtpAuthUriCodec::format)

    suspend operator fun invoke(entry: Entry): EntryQrExportResult {
        var pending = OwnedChars.EMPTY
        var transferred = false
        suspend fun ready(kind: EntryQrKind, text: String): EntryQrExportResult.Ready {
            pending = OwnedChars.fromString(text)
            activityRecorder.recordUsage(entry.id.value, ActivityType.VIEW)
            return EntryQrExportResult.Ready(kind, pending)
        }

        try {
            val result = logger.inSpan(Operations.VaultQrExport) {
                try {
                    val authorization = when (entry.type) {
                        EntryType.WIFI -> authorizationGate.authorize(
                            AuthorizationScope.Global(AuthenticationPurpose.REVEAL_SECRET),
                        ) {
                            val wifi = sensitiveFieldRepository.readBundle(entry.id).wifi
                                ?: return@authorize EntryQrExportResult.Unavailable
                            when (val formatted = WifiQrPayloadFormatter.format(
                                WifiQrPayload(wifi.ssid, wifi.password.orEmpty(), wifi.securityType, wifi.isHidden),
                            )) {
                                is WifiQrFormatResult.Formatted -> ready(EntryQrKind.Wifi, formatted.content)
                                WifiQrFormatResult.InvalidCredential -> EntryQrExportResult.Unavailable
                            }
                        }
                        EntryType.OTP -> {
                            val config = entry.secret.otp?.config
                                ?: return@inSpan EntryQrExportResult.Unavailable
                            authorizationGate.authorize(
                                AuthorizationScope.SensitiveFields(
                                    entry.id, setOf(SensitiveFieldKey.OTP_SECRET), SensitiveAccessAction.REVEAL,
                                ),
                            ) { permit ->
                                val revealed = sensitiveFieldRepository.reveal(
                                    entry.id, SensitiveFieldKey.OTP_SECRET, SensitiveAccessAction.REVEAL, permit,
                                ) ?: return@authorize EntryQrExportResult.Unavailable
                                val chars = revealed.value.toCharArray()
                                try {
                                    ready(EntryQrKind.Otp, formatOtpUri(config.copy(secret = String(chars)), entry.title))
                                } finally {
                                    chars.fill('\u0000')
                                    revealed.value.wipe()
                                }
                            }
                        }
                        else -> return@inSpan EntryQrExportResult.Unavailable
                    }
                    when (authorization) {
                        is AuthorizationResult.Allowed -> authorization.value
                        is AuthorizationResult.Denied -> EntryQrExportResult.Denied
                        AuthorizationResult.Cancelled -> EntryQrExportResult.Cancelled
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    logger.error(
                        Events.Vault.Error,
                        ErrorCodes.Security.VaultOperationFailed,
                        Reasons.UnknownFailure,
                        failure,
                        fields { enum(LogFieldKeys.EntryType, entry.type) },
                    )
                    EntryQrExportResult.Unavailable
                }
            }
            currentCoroutineContext().ensureActive()
            transferred = result is EntryQrExportResult.Ready
            return result
        } finally {
            if (!transferred) pending.wipe()
        }
    }
}
