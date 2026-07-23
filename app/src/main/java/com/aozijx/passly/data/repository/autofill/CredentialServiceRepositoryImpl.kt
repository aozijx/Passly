package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.mapper.VaultEntryCryptoMapper
import com.aozijx.passly.data.mapper.assembler.VaultEntryAssembler
import com.aozijx.passly.data.model.entity.VaultCredentialEntity
import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultCredential
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.VaultMetadata
import com.aozijx.passly.domain.model.entry.WebsiteInfo
import com.aozijx.passly.domain.model.lookup.CredentialCandidate
import com.aozijx.passly.domain.model.lookup.MatchType
import com.aozijx.passly.domain.repository.autofill.CredentialServiceRepository
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialServiceRepositoryImpl @Inject constructor(
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: VaultAccessState,
    private val stateProvider: SessionStateProvider,
    private val cryptoMapper: VaultEntryCryptoMapper,
    private val clock: Clock
) : CredentialServiceRepository {

    override fun search(
        packageName: String?,
        webDomain: String?
    ): List<CredentialCandidate> = runBlocking(Dispatchers.IO) {
        stateProvider.assertWritable()
        sessionManager.query {
            val metadataEntities = metadataDao().getActive()
            val credentialEntities =
                credentialDao().getByEntryIds(metadataEntities.map { it.entryId })
            val credentialMap = credentialEntities.associateBy { it.entryId }

            metadataEntities.filter { it.entryType == EntryType.LOGIN }
                .map { metaEntity ->
                    val meta = cryptoMapper.decryptMetadata(metaEntity)
                    val cred = credentialMap[metaEntity.entryId]
                        ?.let { cryptoMapper.decryptCredential(it) }
                    VaultEntryAssembler.assembleFromDatabase(metaEntity, meta, cred)
                }
                .map { entry ->
                    CredentialCandidate(
                        entry = entry,
                        score = MatchType.UNKNOWN.score,
                        matchedBy = MatchType.UNKNOWN,
                        matchedDomain = entry.associatedDomain,
                        matchedPackage = entry.associatedAppPackage
                    )
                }
        }
    }

    override fun getById(entryId: Int): VaultEntry? = runBlocking(Dispatchers.IO) {
        stateProvider.assertWritable()
        null
    }

    override fun getByIds(entryIds: List<Int>): List<VaultEntry> =
        runBlocking(Dispatchers.IO) {
            stateProvider.assertWritable()
            emptyList()
        }

    override fun updateLastUsed(entryId: Int) {
        runBlocking(Dispatchers.IO) {
            stateProvider.assertWritable()
        }
    }

    override fun save(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): Boolean = runBlocking(Dispatchers.IO) {
        if (sessionState.isLocked()) return@runBlocking false
        sessionManager.query {
            val entryId = UuidCreator.getTimeOrderedEpoch().toString()
            val meta = VaultMetadata(
                entryId = entryId,
                entryType = EntryType.LOGIN,
                title = pageTitle ?: usernameValue,
                username = usernameValue,
                icon = null,
                website = if (webDomain != null) WebsiteInfo(primaryUrl = webDomain) else null
            )
            val cred = VaultCredential(
                entryId = entryId,
                email = null,
                password = passwordValue
            )
            val metaBlob = cryptoMapper.encryptMetadata(meta, entryId)
            val credBlob = cryptoMapper.encryptCredential(cred, entryId)

            val now = clock.now()
            val metaEntity = VaultMetadataEntity(
                entryId = entryId,
                vaultId = "default",
                entryType = EntryType.LOGIN,
                metadataBlob = metaBlob,
                createdAt = now,
                updatedAt = now
            )
            val credEntity = VaultCredentialEntity(
                entryId = entryId,
                credentialBlob = credBlob
            )
            metadataDao().insertStrict(metaEntity)
            credentialDao().insertStrict(credEntity)
        }
        true
    }

}
