package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.mapper.entry.EntryListItemMapper
import com.aozijx.passly.domain.autofill.AutofillDomainNormalization
import com.aozijx.passly.domain.autofill.AutofillScope
import com.aozijx.passly.domain.autofill.model.AUTOFILL_CANDIDATE_LIMIT
import com.aozijx.passly.domain.autofill.model.AutofillCandidateQuery
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.domain.autofill.port.AutofillCandidateRepository
import com.aozijx.passly.domain.entry.model.query.CredentialMatch
import com.aozijx.passly.domain.entry.model.query.MatchType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomAutofillCandidateRepository
    @Inject
    constructor(
        private val databaseSession: AppDatabaseSession,
    ) : AutofillCandidateRepository {
        override suspend fun search(query: AutofillCandidateQuery): List<AutofillCandidateSummary> {
            val applicationId = AutofillScope.normalizeApplicationId(query.packageName)
            val domainNormalization = AutofillScope.normalizeDomain(query.webDomain)
            if (domainNormalization == AutofillDomainNormalization.Invalid) return emptyList()
            val domain = (domainNormalization as? AutofillDomainNormalization.Valid)?.host
            val domainSearchTerm =
                domain?.let {
                    if (query.strictDomainMatching) it else AutofillScope.registrableDomain(it) ?: it
                }
            val candidateApplicationId = applicationId.takeIf { domain == null }
            if (candidateApplicationId == null && domain == null) return emptyList()
            val boundedLimit = query.limit.coerceIn(1, MAX_CANDIDATES)

            return databaseSession.queryAutofill(
                onDenied = { emptyList() },
            ) {
                val matched =
                    buildList {
                        candidateApplicationId?.let {
                            addAll(entryQueryDao().getAutofillCandidatesByApplicationId(it, boundedLimit * 4))
                        }
                        domainSearchTerm?.let {
                            addAll(entryQueryDao().getAutofillCandidatesByDomain(it))
                        }
                    }.distinctBy(EntryEntity::entryId)
                val matchingCandidates =
                    matched.toMatchingAutofillCandidates(
                        candidateApplicationId,
                        domain,
                        query.strictDomainMatching,
                    )
                matchingCandidates
                    .asSequence()
                    .sortedWith(
                        compareByDescending<AutofillCandidateSummary> { it.match.score }
                            .thenByDescending { it.entry.favorite }
                            .thenByDescending { it.entry.updatedAt },
                    ).take(boundedLimit)
                    .toList()
            }
        }

        private companion object {
            const val MAX_CANDIDATES = AUTOFILL_CANDIDATE_LIMIT
        }
    }

internal fun EntryEntity.toAutofillCandidateSummary(match: CredentialMatch): AutofillCandidateSummary =
    AutofillCandidateSummary(
        entry = EntryListItemMapper.fromEntity(this),
        match = match,
    )

internal fun EntryEntity.autofillMatch(
    applicationId: String?,
    domain: String?,
    strictDomainMatching: Boolean = true,
): CredentialMatch {
    if (applicationId != null &&
        applicationIds.any {
            AutofillScope.normalizeApplicationId(it) == applicationId
        }
    ) {
        return CredentialMatch(MatchType.APPLICATION_ID, applicationId = applicationId)
    }
    if (domain != null) {
        val entryDomains =
            buildSet {
                addAll(domains)
                primaryUrl?.let(::add)
            }
        if (entryDomains.any { AutofillScope.matchesDomain(it, domain, strictDomainMatching) }) {
            return CredentialMatch(MatchType.WEB_DOMAIN, domain = domain)
        }
    }
    return CredentialMatch(MatchType.UNKNOWN)
}

internal fun Iterable<EntryEntity>.toMatchingAutofillCandidates(
    applicationId: String?,
    domain: String?,
    strictDomainMatching: Boolean,
): List<AutofillCandidateSummary> =
    mapNotNull { entity ->
        entity
            .autofillMatch(applicationId, domain, strictDomainMatching)
            .takeUnless { it.type == MatchType.UNKNOWN }
            ?.let(entity::toAutofillCandidateSummary)
    }
