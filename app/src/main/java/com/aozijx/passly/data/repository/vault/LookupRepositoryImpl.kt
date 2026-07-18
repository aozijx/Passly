package com.aozijx.passly.data.repository.vault

import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.data.local.database.DatabaseSession
import com.aozijx.passly.data.mapper.assembler.VaultEntryAssembler
import com.aozijx.passly.data.model.entity.VaultCredentialEntity
import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.data.model.serializer.AppJson
import com.aozijx.passly.domain.model.credential.VaultCredential
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.VaultMetadata
import com.aozijx.passly.domain.repository.vault.LookupRepository
import com.aozijx.passly.security.crypto.FieldEncryptor
import com.aozijx.passly.domain.authentication.VaultAccessState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LookupRepositoryImpl @Inject constructor(
    private val sessionManager: DatabaseSession,
    private val sessionState: VaultAccessState,
    private val fieldEncryptor: FieldEncryptor
) : LookupRepository {

    private companion object {
        private val TOTP_ENTRY_TYPES = emptyList<EntryType>()
        private val PASSWORD_ENTRY_TYPES = listOf(
            EntryType.LOGIN,
            EntryType.CARD,
            EntryType.IDENTITY,
            EntryType.NOTE,
            EntryType.WIFI,
            EntryType.SSH_KEY,
            EntryType.CRYPTO_WALLET
        )
    }

    private fun aad(uuid: String, column: String): ByteArray =
        "vault:$uuid:$column".toByteArray(Charsets.UTF_8)

    private fun aadOrNull(uuid: String, column: String): ByteArray? =
        if (uuid.isNotEmpty()) aad(uuid, column) else null

    private fun decryptMetadata(entity: VaultMetadataEntity): VaultMetadata {
        val json =
            fieldEncryptor.decrypt(entity.metadataBlob, aadOrNull(entity.entryId, "metadata"))
        return AppJson.decodeFromString(VaultMetadata.serializer(), json)
    }

    private fun decryptCredential(entity: VaultCredentialEntity): VaultCredential {
        val json =
            fieldEncryptor.decrypt(entity.credentialBlob, aadOrNull(entity.entryId, "credential"))
        return AppJson.decodeFromString(VaultCredential.serializer(), json)
    }

    private fun assembleEntry(
        metaEntity: VaultMetadataEntity,
        credEntity: VaultCredentialEntity?
    ): VaultEntry? {
        return try {
            val meta = decryptMetadata(metaEntity)
            val cred = credEntity?.let { decryptCredential(it) }
            VaultEntryAssembler.assembleFromDatabase(metaEntity, meta, cred)
        } catch (e: Exception) {
            Logcat.w("LookupRepo", "Skipping corrupt entry ${metaEntity.entryId}: ${e.message}")
            null
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val allCategories: Flow<List<String>> = sessionState.isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.withDatabase {
                metadataDao().observeActive()
                    .map { metaEntities ->
                        val credEntities =
                            credentialDao().getByEntryIds(metaEntities.map { it.entryId })
                        val credMap = credEntities.associateBy { it.entryId }
                        metaEntities.mapNotNull { assembleEntry(it, credMap[it.entryId]) }
                            .mapNotNull { it.category.takeIf { c -> c.isNotEmpty() } }
                            .distinct()
                    }
                    .flowOn(Dispatchers.IO)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeEntrySummariesByDemand(
        query: String, category: String?, filter: LookupRepository.EntryFilter
    ): Flow<List<VaultEntry>> = sessionState.isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.withDatabase {
                val entryFlow = when (filter) {
                    LookupRepository.EntryFilter.ALL -> metadataDao().observeActive()
                    LookupRepository.EntryFilter.TOTP_ONLY -> metadataDao().observeByEntryTypes(
                        TOTP_ENTRY_TYPES
                    )

                    LookupRepository.EntryFilter.PASSWORD_ONLY -> metadataDao().observeByEntryTypes(
                        PASSWORD_ENTRY_TYPES
                    )
                }
                entryFlow
                    .map { metaEntities ->
                        val credEntities =
                            credentialDao().getByEntryIds(metaEntities.map { it.entryId })
                        val credMap = credEntities.associateBy { it.entryId }
                        metaEntities.mapNotNull { assembleEntry(it, credMap[it.entryId]) }
                            .filter { entry ->
                                ((query.isEmpty() || entry.title.contains(
                                    query,
                                    ignoreCase = true
                                )
                                        || entry.username.contains(query, ignoreCase = true)
                                        || entry.credential.email?.contains(
                                    query,
                                    ignoreCase = true
                                ) == true
                                        || entry.category.contains(
                                    query,
                                    ignoreCase = true
                                )) || entry.tags.any {
                                    it.contains(
                                        query,
                                        ignoreCase = true
                                    )
                                })
                            }
                            .filter { entry ->
                                category == null || entry.category == category
                            }
                            .filter { entry ->
                                when (filter) {
                                    LookupRepository.EntryFilter.ALL -> true
                                    LookupRepository.EntryFilter.PASSWORD_ONLY -> entry.credential.twoFactor?.otp?.secret.isNullOrEmpty()
                                    LookupRepository.EntryFilter.TOTP_ONLY -> !entry.credential.twoFactor?.otp?.secret.isNullOrEmpty()
                                }
                            }
                            .sortedWith(
                                compareByDescending<VaultEntry> { it.favorite }
                                    .thenByDescending { it.usageCount }
                                    .thenByDescending { it.createdAt }
                            )
                    }
                    .flowOn(Dispatchers.IO)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCategoriesByFilter(filter: LookupRepository.EntryFilter): Flow<List<String>> =
        sessionState.isAuthorized
            .flatMapLatest { authorized ->
                if (!authorized) flowOf(emptyList())
                else sessionManager.withDatabase {
                    val entryFlow = when (filter) {
                        LookupRepository.EntryFilter.ALL -> metadataDao().observeActive()
                        LookupRepository.EntryFilter.TOTP_ONLY -> metadataDao().observeByEntryTypes(
                            TOTP_ENTRY_TYPES
                        )

                        LookupRepository.EntryFilter.PASSWORD_ONLY -> metadataDao().observeByEntryTypes(
                            PASSWORD_ENTRY_TYPES
                        )
                    }
                    entryFlow
                        .map { metaEntities ->
                            val credEntities =
                                credentialDao().getByEntryIds(metaEntities.map { it.entryId })
                            val credMap = credEntities.associateBy { it.entryId }
                            metaEntities.mapNotNull { assembleEntry(it, credMap[it.entryId]) }
                                .filter { entry ->
                                    when (filter) {
                                        LookupRepository.EntryFilter.ALL -> true
                                        LookupRepository.EntryFilter.PASSWORD_ONLY -> entry.credential.twoFactor?.otp?.secret.isNullOrEmpty()
                                        LookupRepository.EntryFilter.TOTP_ONLY -> !entry.credential.twoFactor?.otp?.secret.isNullOrEmpty()
                                    }
                                }
                                .mapNotNull { it.category.takeIf { c -> c.isNotEmpty() } }
                                .distinct()
                        }
                        .flowOn(Dispatchers.IO)
                }
            }
}
