package com.aozijx.passly.feature.vault.otp

import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.feature.vault.copy.SensitiveCopyResult
import com.aozijx.passly.feature.vault.copy.sensitiveCopyDeliveryGuard
import javax.inject.Inject

internal class CopyOtpCodeUseCase
    @Inject
    constructor(
        private val authorizationGate: AuthorizationGate,
        private val clipboardWriter: SensitiveClipboardWriter,
        private val activityRecorder: ActivityRecorder,
        private val otpRuntime: OtpCodeRuntimePort,
    ) {
        suspend operator fun invoke(
            entryId: EntryId,
            canDeliver: () -> Boolean,
        ): SensitiveCopyResult {
            val canWrite = sensitiveCopyDeliveryGuard(canDeliver)
            val result =
                when (
                    val authorization =
                        authorizationGate.authorize(
                            AuthorizationScope.Global(AuthenticationPurpose.COPY_SECRET),
                        ) {
                            if (!canWrite()) return@authorize SensitiveCopyResult.Unavailable
                            val code =
                                otpRuntime.codeForCopy(entryId.value)?.takeIf { it.isNotBlank() && '-' !in it }
                                    ?: return@authorize SensitiveCopyResult.Unavailable
                            if (clipboardWriter.writeSensitiveIf(code, canWrite)) {
                                SensitiveCopyResult.Copied
                            } else {
                                SensitiveCopyResult.Unavailable
                            }
                        }
                ) {
                    is AuthorizationResult.Allowed -> authorization.value

                    is AuthorizationResult.Denied,
                    AuthorizationResult.Cancelled,
                    -> SensitiveCopyResult.NotAuthorized
                }
            if (result == SensitiveCopyResult.Copied && canWrite()) {
                activityRecorder.recordUsage(entryId.value, ActivityType.COPY_OTP_CODE)
            }
            return result
        }
    }
