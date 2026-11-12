package com.aozijx.passly.security.autofill.inbox

import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.access.port.AutofillInboxBootstrapStore
import com.aozijx.passly.domain.autofill.port.AutofillInboxDraftImporter
import com.aozijx.passly.domain.autofill.port.AutofillInboxImportOutcome
import com.aozijx.passly.domain.autofill.port.AutofillInboxRecordStore
import com.aozijx.passly.security.dek.DekManager
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal fun interface AutofillInboxKeyProvisioning {
    suspend fun provisionAfterUnlock()
}

@Singleton
internal class AutofillInboxKeyProvisioner
    @Inject
    constructor(
        private val dekManager: DekManager,
        private val bootstrapStore: AutofillInboxBootstrapStore,
        private val recordStore: AutofillInboxRecordStore,
        private val keyMaterialCodec: AutofillInboxKeyMaterialCodec,
        private val recordCodec: AutofillInboxRecordCodec,
        private val draftImporter: Provider<AutofillInboxDraftImporter>,
        private val logger: Logger,
    ) : AutofillInboxKeyProvisioning {
        private val mutex = Mutex()

        override suspend fun provisionAfterUnlock() =
            mutex.withLock {
                var failureReason: ReasonCode = Reasons.StorageFailure
                try {
                    val current = bootstrapStore.loadAutofillInboxKeyMaterial()
                    if (current != null) {
                        val pending = recordStore.pending(current.keyId)
                        try {
                            failureReason = Reasons.UnknownFailure
                            val decoded =
                                withContext(Dispatchers.Default) {
                                    dekManager.withDek { dek ->
                                        try {
                                            keyMaterialCodec.withPrivateKey(current, dek) { privateKey ->
                                                pending.mapNotNull { record ->
                                                    try {
                                                        record to recordCodec.open(current, privateKey, record.encodedRecord)
                                                    } catch (cancelled: CancellationException) {
                                                        throw cancelled
                                                    } catch (_: Exception) {
                                                        null
                                                    }
                                                }
                                            }
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (failure: Exception) {
                                            throw ExistingKeyMaterialIntegrityFailure(failure)
                                        }
                                    }
                                }
                            failureReason = Reasons.UnknownFailure
                            for (record in pending) {
                                val draft = decoded.firstOrNull { it.first.draftId == record.draftId }?.second
                                if (draft == null) {
                                    failureReason = Reasons.StorageFailure
                                    recordStore.quarantine(current.keyId, record.draftId)
                                    failureReason = Reasons.UnknownFailure
                                    continue
                                }
                                when (draftImporter.get().import(draft)) {
                                    AutofillInboxImportOutcome.Imported -> {
                                        failureReason = Reasons.StorageFailure
                                        recordStore.remove(current.keyId, record.draftId)
                                        failureReason = Reasons.UnknownFailure
                                    }

                                    AutofillInboxImportOutcome.Invalid -> {
                                        failureReason = Reasons.StorageFailure
                                        recordStore.quarantine(current.keyId, record.draftId)
                                        failureReason = Reasons.UnknownFailure
                                    }

                                    AutofillInboxImportOutcome.Retry -> {
                                        return@withLock
                                    }
                                }
                            }
                        } finally {
                            pending.forEach { it.encodedRecord.fill(0) }
                        }
                        return@withLock
                    }

                    failureReason = Reasons.UnknownFailure
                    val material =
                        withContext(Dispatchers.Default) {
                            dekManager.withDek(keyMaterialCodec::generateAndWrap)
                        }
                    failureReason = Reasons.StorageFailure
                    bootstrapStore.saveAutofillInboxKeyMaterial(material)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    val integrityFailure = failure as? ExistingKeyMaterialIntegrityFailure
                    logger.inSpan(Operations.AutofillInboxKeyProvision) {
                        logger.log(
                            LogStatements.Security.vaultOperationFailed(
                                throwable = integrityFailure?.cause ?: failure,
                                reason = integrityFailure?.let { Reasons.IntegrityFailure } ?: failureReason,
                                degraded = true,
                            ),
                        )
                    }
                }
            }
    }

private class ExistingKeyMaterialIntegrityFailure(
    cause: Exception,
) : Exception(cause)
