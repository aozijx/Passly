package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.model.AutofillCredentialPayload
import com.aozijx.passly.domain.autofill.model.AutofillCredentialResolution
import com.aozijx.passly.domain.autofill.model.AutofillCredentialSelection
import com.aozijx.passly.domain.autofill.port.AutofillCredentialResolver
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
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
        if (!sessionState.hasFullSecureSessionAccess()) {
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
            resolveAutofillCredential(entity, password = password, otpCode = null)
        }
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
