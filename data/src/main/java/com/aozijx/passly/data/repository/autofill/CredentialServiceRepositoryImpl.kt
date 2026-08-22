package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.domain.autofill.AutofillScope
import com.aozijx.passly.domain.autofill.port.CredentialServiceRepository
import com.aozijx.passly.core.platform.PackageUtils
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.query.buildRecentEntryIdIntersectionQuery
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.mapper.entry.EntryAssembler
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.query.CredentialCandidate
import com.aozijx.passly.domain.entry.model.query.CredentialMatch
import com.aozijx.passly.domain.entry.model.query.LookupField
import com.aozijx.passly.domain.entry.model.query.MatchType
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.security.search.BlindIndexer
import com.github.f4b6a3.uuid.UuidCreator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CredentialServiceRepositoryImpl @Inject constructor(
    private val databaseSession: AppDatabaseSession,
    private val sessionState: SecureSessionAccessState,
    private val secretFieldStore: SecretFieldStore,
    private val blindIndexer: BlindIndexer,
    private val entryCommands: EntryCommandRepository,
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
        val applicationId = AutofillScope.normalizeApplicationId(packageName)
        val domain = AutofillScope.normalizeDomain(webDomain)
        val boundedLimit = limit.coerceIn(1, MAX_CANDIDATES)

        return databaseSession.query {
            val indexedIds = buildList {
                applicationId?.let {
                    addAll(findMatchingIds(it, listOf(LookupField.APPLICATION_ID), boundedLimit * 4))
                }
                domain?.let {
                    addAll(findMatchingIds(it, listOf(LookupField.DOMAIN, LookupField.URL), boundedLimit * 4))
                }
            }.distinct()
            val entities = when {
                indexedIds.isNotEmpty() -> entryQueryDao().getByIds(indexedIds)
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

    override suspend fun save(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String,
    ): Boolean {
        if (!sessionState.hasFullSecureSessionAccess()) return false
        if (usernameValue.isBlank() && passwordValue.isBlank()) return false
        val applicationId = AutofillScope.normalizeApplicationId(packageName)
        val domain = AutofillScope.normalizeDomain(webDomain)
        val appLabel = applicationId?.let(packageUtils::getAppMetadata)?.label
        val title = resolveAutofillCredentialTitle(
            applicationId = applicationId,
            appLabel = appLabel,
            domain = domain,
            pageTitle = pageTitle,
            usernameValue = usernameValue,
        )
        val now = System.currentTimeMillis()
        val entry = Entry(
            identity = EntryIdentity(
                id = EntryId(UuidCreator.getTimeOrderedEpoch().toString()),
                type = EntryType.LOGIN,
                timestamps = EntryTimestamps(now),
            ),
            profile = EntryProfile(
                title = title,
                username = usernameValue,
                associations = EntryAssociations(
                    primaryUrl = webDomain?.trim()?.takeIf(String::isNotBlank),
                    domains = setOfNotNull(domain),
                    applicationIds = setOfNotNull(applicationId),
                ),
            ),
            secret = EntrySecret(LoginCredential(password = passwordValue)),
        )
        return entryCommands.createEntry(entry).isSuccess
    }

    private suspend fun AppDatabase.findMatchingIds(
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

internal fun resolveAutofillCredentialTitle(
    applicationId: String?,
    appLabel: String?,
    domain: String?,
    pageTitle: String?,
    usernameValue: String,
): String {
    fun String.isApplicationIdTitle(): Boolean = applicationId != null &&
            (equals(applicationId, ignoreCase = true) ||
                    startsWith("$applicationId/", ignoreCase = true))

    val normalizedAppLabel = appLabel?.trim()
        ?.takeIf(String::isNotBlank)
        ?.takeUnless(String::isApplicationIdTitle)
    val normalizedPageTitle = pageTitle?.trim()
        ?.takeIf { it.any(Char::isLetter) }
        ?.takeUnless(String::isApplicationIdTitle)

    return if (domain != null) {
        normalizedPageTitle ?: domain
    } else {
        normalizedAppLabel
            ?: normalizedPageTitle
            ?: usernameValue.trim().takeIf(String::isNotBlank)
            ?: "Login"
    }
}
