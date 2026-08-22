package com.aozijx.passly.feature.autofill.internal

import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.AutofillScope
import com.aozijx.passly.domain.autofill.model.ResolvedCandidate
import com.aozijx.passly.domain.autofill.port.CredentialServiceRepository
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.otp.OtpGenerator
import com.aozijx.passly.domain.entry.model.query.CredentialCandidate
import com.aozijx.passly.domain.entry.model.query.EntryCapabilities
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.settings.model.AutofillSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrieves matching candidates from the vault.
 */
@Singleton
class CandidateRetriever @Inject constructor(
    private val repository: CredentialServiceRepository,
) {
    suspend fun resolve(
        request: AutofillRequest,
        settings: AutofillSettings,
        includeSecrets: Boolean = false
    ): List<ResolvedCandidate> {
        return resolveByPackage(
            packageName = request.packageName,
            webDomain = request.domain,
            settings = settings,
            includeSecrets = includeSecrets
        )
    }

    suspend fun resolveByPackage(
        packageName: String?,
        webDomain: String?,
        settings: AutofillSettings,
        includeSecrets: Boolean = false,
    ): List<ResolvedCandidate> {
        return repository.search(
            packageName = packageName,
            webDomain = webDomain,
            allowUnmatched = settings.allowUnmatchedSuggestions,
            includeSecrets = includeSecrets,
            limit = settings.normalizedMaxSuggestions,
        ).map { it.toResolved(settings.includeOtp) }
    }

    suspend fun resolveByIds(
        ids: List<String>,
        settings: AutofillSettings,
    ): List<ResolvedCandidate> {
        return repository.getByIds(ids, includeSecrets = true).map { entry ->
            entry.toResolvedCandidate(settings.includeOtp)
        }
    }

    suspend fun resolveSelected(
        entryId: String,
        packageName: String?,
        webDomain: String?,
        settings: AutofillSettings,
    ): ResolvedCandidate? {
        val selected = repository.getById(entryId) ?: return null
        if (
            !settings.allowUnmatchedSuggestions &&
            !AutofillScope.matches(selected, packageName, webDomain)
        ) {
            return null
        }
        return selected.toResolvedCandidate(settings.includeOtp)
    }

    private fun CredentialCandidate.toResolved(includeOtp: Boolean): ResolvedCandidate {
        val entry = this.entry
        val otpPreview = if (includeOtp) OtpGenerator.generateSafe(entry.secret.otp?.config) else null

        return ResolvedCandidate(
            entry = entry.toEntryListItem(otpPreview),
            matchedBy = match.type,
            matchedPackage = match.applicationId,
            matchedDomain = match.domain,
            password = entry.secret.login?.password ?: ""
        )
    }

    private fun Entry.toResolvedCandidate(includeOtp: Boolean): ResolvedCandidate {
        val otpPreview = if (includeOtp) OtpGenerator.generateSafe(this.secret.otp?.config) else null

        return ResolvedCandidate(
            entry = this.toEntryListItem(otpPreview),
            password = secret.login?.password ?: ""
        )
    }

    private fun Entry.toEntryListItem(otpPreview: String?): EntryListItem {
        return EntryListItem(
            identity = this.identity,
            profile = this.profile,
            capabilities = EntryCapabilities.from(this.secret),
            otpPreview = otpPreview
        )
    }

}
