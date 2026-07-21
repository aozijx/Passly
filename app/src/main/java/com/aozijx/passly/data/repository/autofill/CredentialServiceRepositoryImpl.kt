package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.mapper.assembler.VaultEntryAssembler
import com.aozijx.passly.data.model.entity.VaultCredentialEntity
import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.data.model.serializer.AppJson
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultCredential
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.VaultMetadata
import com.aozijx.passly.domain.model.entry.WebsiteInfo
import com.aozijx.passly.domain.model.lookup.CredentialCandidate
import com.aozijx.passly.domain.model.lookup.MatchType
import com.aozijx.passly.domain.repository.autofill.CredentialServiceRepository
import com.aozijx.passly.security.crypto.FieldEncryptor
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialServiceRepositoryImpl @Inject constructor(
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: VaultAccessState,
    private val fieldEncryptor: FieldEncryptor,
    private val clock: Clock
) : CredentialServiceRepository {

    private fun aad(uuid: String, column: String): ByteArray =
        "vault:$uuid:$column".toByteArray(Charsets.UTF_8)

    private fun aadOrNull(uuid: String, column: String): ByteArray? =
        if (uuid.isNotEmpty()) aad(uuid, column) else null

    private suspend fun decryptMetadata(entity: VaultMetadataEntity): VaultMetadata {
        val json =
            fieldEncryptor.decrypt(entity.metadataBlob, aadOrNull(entity.entryId, "metadata"))
        return AppJson.decodeFromString(VaultMetadata.serializer(), json)
    }

    private suspend fun decryptCredential(entity: VaultCredentialEntity): VaultCredential {
        val json =
            fieldEncryptor.decrypt(entity.credentialBlob, aadOrNull(entity.entryId, "credential"))
        return AppJson.decodeFromString(VaultCredential.serializer(), json)
    }

    private suspend fun encryptMetadata(meta: VaultMetadata, uuid: String): ByteArray {
        val json = AppJson.encodeToString(VaultMetadata.serializer(), meta)
        return fieldEncryptor.encrypt(json, aad(uuid, "metadata"))
    }

    private suspend fun encryptCredential(cred: VaultCredential, uuid: String): ByteArray {
        val json = AppJson.encodeToString(VaultCredential.serializer(), cred)
        return fieldEncryptor.encrypt(json, aad(uuid, "credential"))
    }

    private suspend fun assembleEntry(
        metaEntity: VaultMetadataEntity,
        credEntity: VaultCredentialEntity?
    ): VaultEntry {
        val meta = decryptMetadata(metaEntity)
        val cred = credEntity?.let { decryptCredential(it) }
        return VaultEntryAssembler.assembleFromDatabase(metaEntity, meta, cred)
    }

    override fun search(
        packageName: String?,
        webDomain: String?
    ): List<CredentialCandidate> = runBlocking(Dispatchers.IO) {
        if (sessionState.isLocked()) return@runBlocking emptyList()
        sessionManager.read {
            val metadataEntities = metadataDao().getActive()
            val credentialEntities =
                credentialDao().getByEntryIds(metadataEntities.map { it.entryId })
            val credentialMap = credentialEntities.associateBy { it.entryId }

            metadataEntities.filter { it.entryType == EntryType.LOGIN }
                .map { assembleEntry(it, credentialMap[it.entryId]) }
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
        if (sessionState.isLocked()) return@runBlocking null
        null
    }

    override fun getByIds(entryIds: List<Int>): List<VaultEntry> =
        runBlocking(Dispatchers.IO) {
            if (sessionState.isLocked()) return@runBlocking emptyList()
            emptyList()
        }

    override fun updateLastUsed(entryId: Int) {
        runBlocking(Dispatchers.IO) {
            if (sessionState.isLocked()) return@runBlocking
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
        sessionManager.read {
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
            val metaBlob = encryptMetadata(meta, entryId)
            val credBlob = encryptCredential(cred, entryId)

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
            metadataDao().insert(metaEntity)
            credentialDao().insert(credEntity)
        }
        true
    }

}
