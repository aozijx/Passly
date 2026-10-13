package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.model.AutofillCredentialPayload
import com.aozijx.passly.domain.autofill.model.AutofillCredentialResolution
import com.aozijx.passly.domain.autofill.model.AutofillCredentialSelection
import com.aozijx.passly.domain.autofill.port.AutofillCredentialResolver
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.otp.OtpGenerator
import com.aozijx.passly.domain.entry.otp.OtpResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomAutofillCredentialResolver @Inject constructor(
    private val databaseSession: AppDatabaseSession,
    private val sessionState: SecureSessionAccessState,
    private val secretFieldStore: SecretFieldStore,
) : AutofillCredentialResolver {
    override suspend fun resolve(
        selection: AutofillCredentialSelection,
    ): AutofillCredentialResolution {
        if (!sessionState.hasAutofillSessionAccess()) {
            return AutofillCredentialResolution.NotFound
        }
        return databaseSession.query {
            val entity = entryQueryDao().getById(selection.entryId.value)
            if (entity == null || entity.deletedAt != null || entity.entryType != EntryType.LOGIN) {
                return@query resolveAutofillCredential(entity, password = null, otpCode = null)
            }
            val password = secretFieldStore.readField(
                db = this,
                entryId = entity.entryId,
                key = SensitiveFieldKey.PASSWORD,
            )
            val otpCode = if (selection.includeOtp) {
                val otpEntryId = findLinkedOtpEntryId(
                    loginEntryId = entity.entryId,
                    links = entryLinkQueryDao().getByEntryId(entity.entryId),
                )
                otpEntryId?.let { linkedEntryId ->
                    val otpEntity = entryQueryDao().getById(linkedEntryId)
                    if (
                        otpEntity == null ||
                        otpEntity.deletedAt != null ||
                        otpEntity.entryType != EntryType.OTP
                    ) {
                        return@let null
                    }
                    val config = secretFieldStore.readBundle(this, linkedEntryId).otp?.config
                    val secret = secretFieldStore.readField(
                        db = this,
                        entryId = linkedEntryId,
                        key = SensitiveFieldKey.OTP_SECRET,
                    )
                    generateAutofillOtpCode(true, config, secret)
                }
            } else {
                null
            }
            resolveAutofillCredential(entity, password = password, otpCode = otpCode)
        }
    }
}

internal fun findLinkedOtpEntryId(
    loginEntryId: String,
    links: List<EntryLinkEntity>,
): String? = links.firstOrNull {
    it.relationType == EntryRelationType.OTP_FOR && it.targetEntryId == loginEntryId
}?.sourceEntryId

internal fun generateAutofillOtpCode(
    includeOtp: Boolean,
    config: OtpConfig?,
    secret: String?,
    timestampSeconds: Long = System.currentTimeMillis() / 1000,
): String? {
    if (!includeOtp || config == null || secret.isNullOrBlank()) return null
    return when (
        val result = OtpGenerator.generate(
            config = config.copy(secret = secret),
            timestamp = timestampSeconds,
        )
    ) {
        is OtpResult.Success -> result.code
        is OtpResult.Failure -> null
    }
}

internal fun resolveAutofillCredential(
    entity: EntryEntity?,
    password: String?,
    otpCode: String?,
): AutofillCredentialResolution {
    if (entity == null) return AutofillCredentialResolution.NotFound
    if (entity.deletedAt != null || entity.entryType != EntryType.LOGIN || password.isNullOrBlank()) {
        return AutofillCredentialResolution.NotFillable
    }
    return AutofillCredentialResolution.Resolved(
        AutofillCredentialPayload(
            entryId = EntryId(entity.entryId),
            username = entity.username,
            password = password,
            otpCode = otpCode,
        ),
    )
}
