package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.mapper.entry.EntryListItemMapper
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.AutofillDomainNormalization
import com.aozijx.passly.domain.autofill.AutofillScope
import com.aozijx.passly.domain.autofill.model.AutofillCandidateQuery
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.domain.autofill.port.AutofillCandidateRepository
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.CredentialMatch
import com.aozijx.passly.domain.entry.model.query.MatchType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomAutofillCandidateRepository
    @Inject
    constructor(
        private val databaseSession: AppDatabaseSession,
        private val sessionState: SecureSessionAccessState,
    ) : AutofillCandidateRepository {
        override suspend fun search(query: AutofillCandidateQuery): List<AutofillCandidateSummary> {
            if (!sessionState.hasAutofillSessionAccess()) return emptyList()
            val applicationId = AutofillScope.normalizeApplicationId(query.packageName)
            val domainNormalization = AutofillScope.normalizeDomain(query.webDomain)
            if (domainNormalization == AutofillDomainNormalization.Invalid) return emptyList()
            val domain = (domainNormalization as? AutofillDomainNormalization.Valid)?.host
            val candidateApplicationId = applicationId.takeIf { domain == null }
            if (candidateApplicationId == null && domain == null) return emptyList()
            val boundedLimit = query.limit.coerceIn(1, MAX_CANDIDATES)

            return databaseSession.query {
                val matched =
                    buildList {
                        candidateApplicationId?.let {
                            addAll(entryQueryDao().getAutofillCandidatesByApplicationId(it, boundedLimit * 4))
                        }
                        domain?.let {
                            addAll(entryQueryDao().getAutofillCandidatesByDomain(it))
                        }
                    }.distinctBy(EntryEntity::entryId)
                val exactMatches =
                    matched.mapNotNull { entity ->
                        entity
                            .autofillMatch(candidateApplicationId, domain)
                            .takeUnless { it.type == MatchType.UNKNOWN }
                            ?.let { match -> entity.toAutofillCandidateSummary(match) }
                    }
                val candidates =
                    when {
                        exactMatches.isNotEmpty() -> {
                            exactMatches
                        }

                        query.allowUnmatched -> {
                            entryQueryDao()
                                .getActiveByType(EntryType.LOGIN)
                                .take(boundedLimit)
                                .map { it.toAutofillCandidateSummary(CredentialMatch(MatchType.UNKNOWN)) }
                        }

                        else -> {
                            emptyList()
                        }
                    }

                candidates
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
            const val MAX_CANDIDATES = 10
        }
    }

internal fun EntryEntity.toAutofillCandidateSummary(match: CredentialMatch): AutofillCandidateSummary =
    AutofillCandidateSummary(
        entry = EntryListItemMapper.assemble(this, EntryProfileMapper.fromEntity(this)),
        match = match,
    )

internal fun EntryEntity.autofillMatch(
    applicationId: String?,
    domain: String?,
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
        if (entryDomains.any { AutofillScope.normalizeDomain(it) == AutofillDomainNormalization.Valid(domain) }) {
            return CredentialMatch(MatchType.WEB_DOMAIN, domain = domain)
        }
    }
    return CredentialMatch(MatchType.UNKNOWN)
}
