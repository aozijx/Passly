package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.mapper.entry.EntryAggregateAssembler
import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.data.model.entity.EntrySecretEntity
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.entry.EntryCapabilityFlags
import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.EntrySummary
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.WebsiteInfo
import com.aozijx.passly.domain.model.entry.secret.LoginSecret
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
    private val summaryCodec: EntrySummaryCodec,
    private val secretCodec: EntrySecretCodec,
    private val clock: Clock
) : CredentialServiceRepository {

    override fun search(
        packageName: String?,
        webDomain: String?
    ): List<CredentialCandidate> = runBlocking(Dispatchers.IO) {
        sessionManager.query {
            val metadataEntities = entryQueryDao().getActive()
            val credentialEntities =
                entrySecretQueryDao().getByEntryIds(metadataEntities.map { it.entryId })
            val credentialMap = credentialEntities.associateBy { it.entryId }

            metadataEntities.filter { it.entryType == EntryType.LOGIN }
                .map { metaEntity ->
                    val summary = summaryCodec.decrypt(metaEntity.summaryBlob, metaEntity.entryId)
                    val secret = credentialMap[metaEntity.entryId]
                        ?.let { secretCodec.decrypt(it.secretBlob, it.entryId) }
                    EntryAggregateAssembler.assembleFromDatabase(metaEntity, summary, secret)
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
        null
    }

    override fun getByIds(entryIds: List<Int>): List<VaultEntry> =
        runBlocking(Dispatchers.IO) {
            emptyList()
        }

    override fun updateLastUsed(entryId: Int) {
        runBlocking(Dispatchers.IO) {
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
            val summary = EntrySummary(
                title = pageTitle ?: usernameValue,
                username = usernameValue,
                icon = null,
                website = if (webDomain != null) WebsiteInfo(primaryUrl = webDomain) else null
            )
            val secret = EntrySecret(
                login = LoginSecret(
                    email = null,
                    password = passwordValue
                )
            )
            val metaBlob = summaryCodec.encrypt(summary, entryId)
            val credBlob = secretCodec.encrypt(secret, entryId)

            val now = clock.now()
            val capabilityFlags = EntryCapabilityFlags.computeFrom(secret)
            val otpType = EntryCapabilityFlags.otpTypeFrom(secret)
            val metaEntity = EntryEntity(
                entryId = entryId,
                vaultId = "default",
                entryType = EntryType.LOGIN,
                capabilityFlags = capabilityFlags,
                otpType = otpType,
                summaryBlob = metaBlob,
                createdAt = now,
                updatedAt = now
            )
            val credEntity = EntrySecretEntity(
                entryId = entryId,
                secretBlob = credBlob
            )
            entryCommandDao().insertStrict(metaEntity)
            entrySecretCommandDao().insertStrict(credEntity)
        }
        true
    }

}
