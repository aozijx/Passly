package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.core.platform.PackageUtils
import com.aozijx.passly.data.local.database.session.UnifiedSessionManager
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.local.dao.buildRecentEntryIdIntersectionQuery
import com.aozijx.passly.data.mapper.entry.EntryAggregateAssembler
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.AutofillConfiguration
import com.aozijx.passly.domain.autofill.policy.AutofillTitlePolicy
import com.aozijx.passly.domain.autofill.policy.CredentialScopeMatcher
import com.aozijx.passly.domain.autofill.repository.CredentialServiceRepository
import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.model.WebsiteInfo
import com.aozijx.passly.domain.entry.model.lookup.CredentialCandidate
import com.aozijx.passly.domain.entry.model.lookup.LookupField
import com.aozijx.passly.domain.entry.model.lookup.MatchType
import com.aozijx.passly.domain.entry.model.secret.LoginSecret
import com.aozijx.passly.domain.entry.repository.EntryCommandRepository
import com.aozijx.passly.security.search.BlindIndexer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Autofill/Credential Manager read model.
 *
 * Candidate lookup first uses the encrypted blind index, then decrypts only the
 * small matched set and verifies the association against the plaintext request.
 */
@Singleton
class CredentialServiceRepositoryImpl @Inject constructor(
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: SecureSessionAccessState,
    private val summaryCodec: EntrySummaryCodec,
    private val secretCodec: EntrySecretCodec,
    private val blindIndexer: BlindIndexer,
    private val entryCommandRepository: EntryCommandRepository,
    private val packageUtils: PackageUtils,
) : CredentialServiceRepository {

    override suspend fun search(
        packageName: String?,
        webDomain: String?,
        allowUnmatched: Boolean,
        includeSecrets: Boolean,
        limit: Int,
    ): List<CredentialCandidate> {
        if (!sessionState.hasFullSecureSessionAccess()) return emptyList()

        val normalizedPackage = CredentialScopeMatcher.normalizePackage(packageName)
        val normalizedDomain = CredentialScopeMatcher.normalizeDomain(webDomain)
        val boundedLimit = limit.coerceIn(1, AutofillConfiguration.MAX_CANDIDATES)

        return sessionManager.query {
            val packageMatches = normalizedPackage
                ?.let {
                    findMatchingIds(
                        it,
                        listOf(LookupField.PACKAGE),
                        boundedLimit * INDEX_PREFETCH_FACTOR,
                    )
                }
                .orEmpty()
            val domainMatches = normalizedDomain
                ?.let {
                    findMatchingIds(
                        it,
                        listOf(LookupField.DOMAIN, LookupField.URL),
                        boundedLimit * INDEX_PREFETCH_FACTOR,
                    )
                }
                .orEmpty()

            val indexedIds = (packageMatches + domainMatches).distinct()
            val entities = when {
                indexedIds.isNotEmpty() -> entryQueryDao().getByIds(indexedIds)
                allowUnmatched -> entryQueryDao()
                    .getActiveByType(EntryType.LOGIN)
                    .take(boundedLimit)

                else -> emptyList()
            }
            if (entities.isEmpty()) return@query emptyList()

            val secretMap = if (includeSecrets) {
                entrySecretQueryDao()
                    .getByEntryIds(entities.map { it.entryId })
                    .associateBy { it.entryId }
            } else {
                emptyMap()
            }

            entities
                .filter { it.deletedAt == null && AutofillConfiguration.isAutofillSupported(it.entryType) }
                .map { entity ->
                    val summary = summaryCodec.decrypt(entity.summaryBlob, entity.entryId)
                    val secret = secretMap[entity.entryId]
                        ?.let { secretCodec.decrypt(it.secretBlob, it.entryId) }
                    EntryAggregateAssembler.assembleFromDatabase(entity, summary, secret)
                }
                .mapNotNull { entry ->
                    val matchType = CredentialScopeMatcher.matchType(
                        entry,
                        normalizedPackage,
                        normalizedDomain,
                    )
                    if (matchType == MatchType.UNKNOWN && !allowUnmatched) return@mapNotNull null
                    CredentialCandidate(
                        entry = entry,
                        score = matchType.score,
                        matchedBy = matchType,
                        matchedPackage = normalizedPackage.takeIf {
                            matchType == MatchType.PACKAGE_NAME
                        },
                        matchedDomain = normalizedDomain.takeIf {
                            matchType == MatchType.WEB_DOMAIN
                        },
                    )
                }
                .sortedWith(AutofillConfiguration::compareCandidates)
                .take(boundedLimit)
                .toList()
        }
    }

    override suspend fun getById(entryId: String): EntryAggregate? =
        getByIds(listOf(entryId), includeSecrets = true).firstOrNull()

    override suspend fun getByIds(
        entryIds: List<String>,
        includeSecrets: Boolean
    ): List<EntryAggregate> {
        if (!sessionState.hasFullSecureSessionAccess() || entryIds.isEmpty()) return emptyList()
        val uniqueIds = entryIds.distinct()
        return sessionManager.query {
            val entries = entryQueryDao().getByIds(uniqueIds)
                .filter { it.deletedAt == null }
            val secretMap = if (includeSecrets) {
                entrySecretQueryDao()
                    .getByEntryIds(entries.map { it.entryId })
                    .associateBy { it.entryId }
            } else {
                emptyMap()
            }
            val assembled = entries.associate { entity ->
                val summary = summaryCodec.decrypt(entity.summaryBlob, entity.entryId)
                val secret = secretMap[entity.entryId]
                    ?.let { secretCodec.decrypt(it.secretBlob, it.entryId) }
                entity.entryId to EntryAggregateAssembler.assembleFromDatabase(
                    entity,
                    summary,
                    secret,
                )
            }
            uniqueIds.mapNotNull(assembled::get)
        }
    }

    override suspend fun save(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String,
    ): Boolean {
        if (!sessionState.hasFullSecureSessionAccess()) return false
        if (usernameValue.isBlank() && passwordValue.isBlank()) return false

        val normalizedPackage = CredentialScopeMatcher.normalizePackage(packageName)
        val normalizedDomain = CredentialScopeMatcher.normalizeDomain(webDomain)
        val appLabel = normalizedPackage
            ?.let(packageUtils::getAppMetadata)
            ?.appName
        val entry = EntryAggregate(
            header = EntryHeader(
                id = EntryId(""),
                entryType = EntryType.LOGIN,
                version = EntryVersion.INITIAL,
                createdAt = 0L,
                updatedAt = 0L,
            ),
            summary = EntrySummary(
                title = AutofillTitlePolicy.resolveSavedCredentialTitle(
                    pageTitle = pageTitle,
                    domain = normalizedDomain,
                    appLabel = appLabel,
                    packageName = normalizedPackage,
                    fallback = usernameValue.ifBlank { "Login" }
                ),
                username = usernameValue,
                website = WebsiteInfo(
                    primaryUrl = webDomain?.trim()?.takeIf { it.isNotBlank() },
                    matchDomains = setOfNotNull(normalizedDomain),
                    packageNames = setOfNotNull(normalizedPackage),
                ),
            ),
            secret = EntrySecret(
                login = LoginSecret(password = passwordValue),
            ),
        )
        return entryCommandRepository.createEntry(entry).isSuccess
    }

    private suspend fun com.aozijx.passly.data.local.database.AppDatabase.findMatchingIds(
        value: String,
        fields: List<LookupField>,
        limit: Int,
    ): List<String> {
        val tokens = blindIndexer.searchTokens(value)
        if (tokens.isEmpty()) return emptyList()
        return searchTokenQueryDao().searchByTokenIntersection(
            buildRecentEntryIdIntersectionQuery(tokens, fields, limit)
        )
    }

    private companion object {
        const val INDEX_PREFETCH_FACTOR = 4
    }
}
