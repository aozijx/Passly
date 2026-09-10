package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.mapper.entry.EntryAssembler
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.AutofillScope
import com.aozijx.passly.domain.autofill.port.AutofillCredentialRepository
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.CredentialCandidate
import com.aozijx.passly.domain.entry.model.query.CredentialMatch
import com.aozijx.passly.domain.entry.model.query.MatchType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AutofillCredentialRepositoryImpl @Inject constructor(
    private val databaseSession: AppDatabaseSession,
    private val sessionState: SecureSessionAccessState,
    private val secretFieldStore: SecretFieldStore,
) : AutofillCredentialRepository {
    override suspend fun search(
        packageName: String?,
        webDomain: String?,
        allowUnmatched: Boolean,
        includeSecrets: Boolean,
        limit: Int,
    ): List<CredentialCandidate> {
        if (!sessionState.hasFullSecureSessionAccess()) return emptyList()
        val applicationId = AutofillScope.normalizeApplicationId(packageName)
        val domain = AutofillScope.normalizeDomain(webDomain)
        val boundedLimit = limit.coerceIn(1, MAX_CANDIDATES)

        return databaseSession.query {
            val candidates = buildList {
                applicationId?.let {
                    addAll(entryQueryDao().getAutofillCandidatesByApplicationId(it, boundedLimit * 4))
                }
                domain?.let {
                    addAll(entryQueryDao().getAutofillCandidatesByDomain(it, boundedLimit * 4))
                }
            }.distinctBy { it.entryId }
            val entities = when {
                candidates.isNotEmpty() -> candidates
                allowUnmatched -> entryQueryDao().getActiveByType(EntryType.LOGIN).take(boundedLimit)
                else -> emptyList()
            }
            val secrets = if (includeSecrets) {
                entities.associateBy(
                    keySelector = { it.entryId },
                    valueTransform = { secretFieldStore.readAll(this, it.entryId) },
                )
            } else {
                emptyMap()
            }

            entities
                .filter { (it.deletedAt == null) && (it.entryType == EntryType.LOGIN) }
                .mapNotNull { entity ->
                    val secret = if (includeSecrets) {
                        secrets[entity.entryId] ?: return@mapNotNull null
                    } else {
                        EntrySecret()
                    }
                    EntryAssembler.assembleFromDatabase(
                        entity,
                        EntryProfileMapper.fromEntity(entity),
                        secret,
                    )
                }
                .asSequence()
                .mapNotNull { entry ->
                    val match = entry.match(applicationId, domain)
                    if (match.type == MatchType.UNKNOWN && !allowUnmatched) null
                    else CredentialCandidate(entry, match)
                }
                .sortedWith(
                    compareByDescending<CredentialCandidate> { it.match.score }
                        .thenByDescending { it.entry.profile.favorite }
                        .thenByDescending { it.entry.timestamps.updatedAtMs }
                )
                .take(boundedLimit)
                .toList()
        }
    }

    override suspend fun getById(entryId: String): Entry? =
        getByIds(listOf(entryId), includeSecrets = true).firstOrNull()

    override suspend fun getByIds(entryIds: List<String>, includeSecrets: Boolean): List<Entry> {
        if (!sessionState.hasFullSecureSessionAccess() || entryIds.isEmpty()) return emptyList()
        val uniqueIds = entryIds.distinct()
        return databaseSession.query {
            val entities = entryQueryDao().getByIds(uniqueIds).filter { it.deletedAt == null }
            val entries = entities.associate { entity ->
                val fullSecret = secretFieldStore.readAll(this, entity.entryId)
                entity.entryId to EntryAssembler.assembleFromDatabase(
                    entity,
                    EntryProfileMapper.fromEntity(entity),
                    if (includeSecrets) fullSecret else fullSecret.redacted(),
                )
            }
            uniqueIds.mapNotNull(entries::get)
        }
    }

    private fun Entry.match(applicationId: String?, domain: String?): CredentialMatch {
        if (applicationId != null && profile.associations.applicationIds.any {
                AutofillScope.normalizeApplicationId(it) == applicationId
            }) {
            return CredentialMatch(MatchType.APPLICATION_ID, applicationId = applicationId)
        }
        if (domain != null) {
            val entryDomains = buildSet {
                addAll(profile.associations.domains)
                profile.associations.primaryUrl?.let(::add)
            }
            if (entryDomains.any { AutofillScope.normalizeDomain(it) == domain }) {
                return CredentialMatch(MatchType.WEB_DOMAIN, domain = domain)
            }
        }
        return CredentialMatch(MatchType.UNKNOWN)
    }

    private fun EntrySecret.redacted(): EntrySecret = copy(
        credential = login?.copy(password = null) ?: credential,
    )

    private companion object {
        const val MAX_CANDIDATES = 10
    }
}
