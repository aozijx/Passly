package com.aozijx.passly.data.repository.otp

import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerationMaterial
import com.aozijx.passly.domain.entry.otp.generation.OtpResult
import com.aozijx.passly.domain.entry.port.OtpConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomOtpConfigRepository
    @Inject
    constructor(
        private val databaseSession: AppDatabaseSession,
        private val secretFieldStore: SecretFieldStore,
        private val hotpCodeConsumption: HotpCodeConsumptionTransaction,
        private val hotpCounterRollback: HotpCounterRollbackTransaction,
    ) : OtpConfigRepository {
        override suspend fun getGenerationMaterial(entryId: String): OtpGenerationMaterial? =
            databaseSession.queryFullVault(
                onDenied = { null },
            ) {
                val entry = entryQueryDao().getById(entryId) ?: return@queryFullVault null
                secretFieldStore.readOtpGenerationMaterial(this, entry)
            }

        override suspend fun consumeHotpCode(
            entryId: String,
            expectedCounter: Long,
        ): OtpResult =
            databaseSession.transactionFullVault(
                onDenied = { OtpResult.Failure(OtpGenerationError.InvalidSecret) },
            ) { hotpCodeConsumption.consume(this, entryId, expectedCounter) }

        override suspend fun rollbackHotpCounter(
            entryId: String,
            expectedCurrentCounter: Long,
        ): Boolean =
            databaseSession.transactionFullVault(
                onDenied = { false },
            ) { hotpCounterRollback.rollback(this, entryId, expectedCurrentCounter) }
    }
