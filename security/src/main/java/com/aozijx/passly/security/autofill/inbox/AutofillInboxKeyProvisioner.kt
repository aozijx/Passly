package com.aozijx.passly.security.autofill.inbox

import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.access.port.AutofillInboxBootstrapStore
import com.aozijx.passly.domain.access.port.AutofillInboxRecordStore
import com.aozijx.passly.domain.autofill.port.AutofillInboxDraftImporter
import com.aozijx.passly.domain.autofill.port.AutofillInboxImportOutcome
import com.aozijx.passly.security.dek.DekManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal fun interface AutofillInboxKeyProvisioning {
    suspend fun provisionAfterUnlock()
}

@Singleton
internal class AutofillInboxKeyProvisioner @Inject constructor(
    private val dekManager: DekManager,
    private val bootstrapStore: AutofillInboxBootstrapStore,
    private val recordStore: AutofillInboxRecordStore,
    private val keyMaterialCodec: AutofillInboxKeyMaterialCodec,
    private val recordCodec: AutofillInboxRecordCodec,
    private val draftImporter: Provider<AutofillInboxDraftImporter>,
    private val logger: Logger,
) : AutofillInboxKeyProvisioning {
    private val mutex = Mutex()

    override suspend fun provisionAfterUnlock() = mutex.withLock {
        var validatingExistingKey = false
        try {
            val current = bootstrapStore.loadAutofillInboxKeyMaterial()
            if (current != null) {
                validatingExistingKey = true
                val pending = recordStore.pending(current.keyId)
                try {
                    val decoded = withContext(Dispatchers.Default) {
                        dekManager.withDek { dek ->
                            keyMaterialCodec.withPrivateKey(current, dek) { privateKey ->
                                pending.mapNotNull { record ->
                                    try {
                                        record to recordCodec.open(current, privateKey, record.encodedRecord)
                                    } catch (_: Exception) {
                                        null
                                    }
                                }
                            }
                        }
                    }
                    for (record in pending) {
                        val draft = decoded.firstOrNull { it.first.draftId == record.draftId }?.second
                        if (draft == null) {
                            recordStore.quarantine(current.keyId, record.draftId)
                            continue
                        }
                        when (draftImporter.get().import(draft)) {
                            AutofillInboxImportOutcome.Imported ->
                                recordStore.remove(current.keyId, record.draftId)
                            AutofillInboxImportOutcome.Invalid ->
                                recordStore.quarantine(current.keyId, record.draftId)
                            AutofillInboxImportOutcome.Retry -> return@withLock
                        }
                    }
                } finally {
                    pending.forEach { it.encodedRecord.fill(0) }
                }
                return@withLock
            }

            val material = withContext(Dispatchers.Default) {
                dekManager.withDek(keyMaterialCodec::generateAndWrap)
            }
            bootstrapStore.saveAutofillInboxKeyMaterial(material)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            logger.inSpan(Operations.AutofillInboxKeyProvision) {
                logger.log(
                    LogStatements.Security.vaultOperationFailed(
                        throwable = IllegalStateException(
                            if (validatingExistingKey) {
                                "Autofill Inbox key material failed integrity validation"
                            } else {
                                "Autofill Inbox key provisioning failed"
                            },
                        ),
                        reason = if (validatingExistingKey) {
                            Reasons.IntegrityFailure
                        } else {
                            Reasons.UnknownFailure
                        },
                        degraded = true,
                    ),
                )
            }
        }
    }
}
