package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.autofill.model.AutofillCredentialField
import com.aozijx.passly.domain.autofill.model.AutofillCredentialPayload
import com.aozijx.passly.domain.autofill.model.AutofillCredentialResolution
import com.aozijx.passly.domain.autofill.model.AutofillCredentialSelection
import com.aozijx.passly.domain.autofill.port.AutofillCredentialResolver
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.MatchType
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerator
import com.aozijx.passly.domain.entry.otp.generation.OtpResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
internal class RoomAutofillCredentialResolver
    @Inject
    constructor(
        private val databaseSession: AppDatabaseSession,
        private val secretFieldStore: SecretFieldStore,
        private val logger: Logger,
    ) : AutofillCredentialResolver {
        override suspend fun resolve(selection: AutofillCredentialSelection): AutofillCredentialResolution {
            return try {
                databaseSession.queryAutofill(
                    onDenied = { AutofillCredentialResolution.NotFound },
                ) {
                    val entity = entryQueryDao().getById(selection.entryId.value)
                    if (entity == null || entity.deletedAt != null || entity.entryType != EntryType.LOGIN) {
                        return@queryAutofill resolveAutofillCredential(
                            entity,
                            selection.requestedFields,
                            emptyMap(),
                        )
                    }
                    val database = this
                    val values =
                        buildMap {
                            if (AutofillCredentialField.USERNAME in selection.requestedFields) {
                                entity.username
                                    .takeIf(String::isNotBlank)
                                    ?.let { put(AutofillCredentialField.USERNAME, it) }
                            }
                            if (AutofillCredentialField.PASSWORD in selection.requestedFields) {
                                secretFieldStore
                                    .readField(database, entity.entryId, SensitiveFieldKey.PASSWORD)
                                    ?.takeIf(String::isNotBlank)
                                    ?.let { put(AutofillCredentialField.PASSWORD, it) }
                            }
                            if (AutofillCredentialField.OTP in selection.requestedFields) {
                                val linkedOtpEntryId =
                                    findLinkedOtpEntryId(
                                        loginEntryId = entity.entryId,
                                        links = entryLinkQueryDao().getByEntryId(entity.entryId),
                                    )
                                val otpEntity =
                                    if (linkedOtpEntryId != null) {
                                        entryQueryDao().getById(linkedOtpEntryId)
                                    } else {
                                        entryQueryDao()
                                            .getActiveByType(EntryType.OTP)
                                            .filter { otpEntry -> otpEntry.matchesAutofillScope(selection) }
                                            .singleOrNull()
                                    }
                                otpEntity?.let { matchedOtp ->
                                    if (
                                        matchedOtp.deletedAt != null ||
                                        matchedOtp.entryType != EntryType.OTP ||
                                        !matchedOtp.matchesAutofillScope(selection)
                                    ) {
                                        return@let
                                    }
                                    secretFieldStore
                                        .readOtpGenerationMaterial(database, matchedOtp)
                                        ?.use { material ->
                                            when (
                                                val result =
                                                    OtpGenerator.generate(
                                                        material,
                                                        timestamp = System.currentTimeMillis() / 1000,
                                                    )
                                            ) {
                                                is OtpResult.Success -> put(AutofillCredentialField.OTP, result.code)
                                                is OtpResult.CounterSuccess -> put(AutofillCredentialField.OTP, result.code)
                                                is OtpResult.Failure -> Unit
                                            }
                                        }
                                }
                            }
                        }
                    resolveAutofillCredential(entity, selection.requestedFields, values)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                try {
                    logger.log(LogStatements.Autofill.discoveryFailed(error))
                } catch (_: Exception) {
                    // Diagnostics must not replace the typed resolver failure.
                }
                throw error
            }
        }
    }

private fun EntryEntity.matchesAutofillScope(selection: AutofillCredentialSelection): Boolean =
    autofillMatch(
        applicationId = selection.scope.packageName.takeIf { selection.scope.webDomain == null },
        domain = selection.scope.webDomain,
        strictDomainMatching = selection.strictDomainMatching,
    ).type != MatchType.UNKNOWN

internal fun findLinkedOtpEntryId(
    loginEntryId: String,
    links: List<EntryLinkEntity>,
): String? =
    links
        .firstOrNull {
            it.relationType == EntryRelationType.OTP_FOR && it.targetEntryId == loginEntryId
        }?.sourceEntryId

internal fun resolveAutofillCredential(
    entity: EntryEntity?,
    requestedFields: Set<AutofillCredentialField>,
    values: Map<AutofillCredentialField, String>,
): AutofillCredentialResolution {
    if (entity == null) return AutofillCredentialResolution.NotFound
    if (entity.deletedAt != null || entity.entryType != EntryType.LOGIN) {
        return AutofillCredentialResolution.NotFillable
    }
    val resolvedFields = values.filter { (field, value) -> field in requestedFields && value.isNotBlank() }
    if (resolvedFields.isEmpty()) return AutofillCredentialResolution.NotFillable
    return AutofillCredentialResolution.Resolved(
        AutofillCredentialPayload(
            entryId = EntryId(entity.entryId),
            fields = resolvedFields,
        ),
    )
}
