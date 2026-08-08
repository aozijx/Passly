package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.mapper.entry.EntryAggregateAssembler
import com.aozijx.passly.data.repository.VaultTransactionRunner
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.repository.EntryHierarchyRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomEntryHierarchyRepository @Inject constructor(
    private val transactionRunner: VaultTransactionRunner,
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: VaultAccessState,
    private val summaryCodec: EntrySummaryCodec,
    private val secretCodec: EntrySecretCodec,
    private val clock: Clock
) : EntryHierarchyRepository {

    override suspend fun assignToAccount(
        entryId: String,
        expectedVersion: Int,
        accountEntryId: String?
    ): AppResult<Unit> = transactionRunner.write("entry.assign-account") {
        val entry = entryQueryDao().getById(entryId)
            ?: throw NotFound()
        require(entry.entryType != EntryType.ACCOUNT) {
            "An ACCOUNT entry cannot be assigned to another ACCOUNT"
        }
        accountEntryId?.let { parentId ->
            require(parentId != entryId) { "An entry cannot own itself" }
            val parent = entryQueryDao().getById(parentId)
                ?: throw NotFound()
            require(parent.entryType == EntryType.ACCOUNT) {
                "Parent entry must be an ACCOUNT"
            }
            require(parent.parentEntryId == null) {
                "An ACCOUNT parent cannot itself be a child"
            }
            require(parent.vaultId == entry.vaultId) {
                "Child and parent must belong to the same vault"
            }
        }
        val affected = entryCommandDao().updateParent(
            entryId = entryId,
            expectedVersion = expectedVersion,
            parentEntryId = accountEntryId,
            updatedAt = clock.now()
        )
        transactionRunner.checkAffectedRows(entryId, expectedVersion, affected)
    }

    override suspend fun getChildren(accountEntryId: String): List<VaultEntry> =
        if (!sessionState.hasFullVaultAccess()) {
            emptyList()
        } else {
            sessionManager.query {
                val parent = entryQueryDao().getById(accountEntryId)
                    ?: throw NotFound()
                require(parent.entryType == EntryType.ACCOUNT) {
                    "Entry is not an ACCOUNT: $accountEntryId"
                }
                val metadata = entryQueryDao().getChildren(accountEntryId)
                val secrets = entrySecretQueryDao()
                    .getByEntryIds(metadata.map { it.entryId })
                    .associateBy { it.entryId }
                metadata.map { entity ->
                    val summary = summaryCodec.decrypt(entity.summaryBlob, entity.entryId)
                    val secret = secrets[entity.entryId]?.let {
                        secretCodec.decrypt(it.secretBlob, it.entryId)
                    }
                    EntryAggregateAssembler.assembleFromDatabase(entity, summary, secret)
                }
            }
        }
}
