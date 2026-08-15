package com.aozijx.passly.core.autofill.pipeline

import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.autofill.model.InternalFillRequest
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.core.otp.OtpGenerator
import com.aozijx.passly.core.otp.OtpResult
import com.aozijx.passly.data.autofill.port.CredentialServiceRepository
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.query.CredentialCandidate
import com.aozijx.passly.data.settings.model.AutofillSettings
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CandidateResolver @Inject constructor(
    private val repository: CredentialServiceRepository,
) {
    companion object {
        private const val TAG = "CandidateResolver"
    }

    suspend fun resolve(
        request: InternalFillRequest,
        settings: AutofillSettings,
    ): List<ResolvedCandidate> {
        return resolveByPackage(request.parentPackage, request.webDomain, settings)
    }

    suspend fun resolveByPackage(
        packageName: String?,
        webDomain: String?,
        settings: AutofillSettings,
        includeSecrets: Boolean? = null,
    ): List<ResolvedCandidate> {
        return try {
            repository.search(
                packageName = packageName,
                webDomain = webDomain,
                allowUnmatched = settings.allowUnmatchedSuggestions,
                includeSecrets = includeSecrets ?: false,
                limit = settings.normalizedMaxSuggestions,
            ).map { it.toResolved(settings.includeOtp) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppTelemetry.e(TAG, "Candidate lookup failed", e)
            emptyList()
        }
    }

    suspend fun resolveByIds(
        ids: List<String>,
        settings: AutofillSettings,
    ): List<ResolvedCandidate> {
        return try {
            repository.getByIds(ids, includeSecrets = false).map { entry ->
                entry.toResolvedCandidate(settings.includeOtp)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppTelemetry.e(TAG, "resolveByIds failed", e)
            emptyList()
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
            !selected.matchesScope(packageName, webDomain)
        ) {
            AppTelemetry.w(TAG, "Rejected selected credential outside request scope")
            return null
        }
        return selected.toResolvedCandidate(settings.includeOtp)
    }

    private fun CredentialCandidate.toResolved(includeOtp: Boolean): ResolvedCandidate {
        val entry = this.entry
        return ResolvedCandidate(
            candidateId = entry.id.value,
            displayName = entry.profile.title,
            username = entry.profile.username,
            password = entry.secret.login?.password ?: "",
            totpCode = if (includeOtp) generateTotpFromEntry(entry) else null,
            associatedDomain = entry.profile.associations.primaryUrl
                ?: entry.profile.associations.domains.firstOrNull(),
            associatedAppPackage = entry.profile.associations.applicationIds.firstOrNull(),
            iconName = entry.profile.icon.name,
            iconCustomPath = entry.profile.icon.customReference,
            entryType = entry.type,
            matchedBy = match.type,
            matchedPackage = match.applicationId,
            matchedDomain = match.domain,
        )
    }

    private fun Entry.toResolvedCandidate(includeOtp: Boolean): ResolvedCandidate {
        return ResolvedCandidate(
            candidateId = id.value,
            displayName = profile.title,
            username = profile.username,
            password = secret.login?.password ?: "",
            totpCode = if (includeOtp) generateTotpFromEntry(this) else null,
            associatedDomain = profile.associations.primaryUrl
                ?: profile.associations.domains.firstOrNull(),
            associatedAppPackage = profile.associations.applicationIds.firstOrNull(),
            iconName = profile.icon.name,
            iconCustomPath = profile.icon.customReference,
            entryType = type,
        )
    }

    private fun generateTotpFromEntry(entry: Entry): String? {
        val otpConfig = entry.secret.otp?.config ?: return null
        if (otpConfig.secret.isNullOrBlank()) return null
        return when (val result = OtpGenerator.generate(otpConfig)) {
            is OtpResult.Success -> result.code
            is OtpResult.Failure -> null
        }
    }

    private fun Entry.matchesScope(packageName: String?, webDomain: String?): Boolean {
        val applicationId = packageName?.trim()?.lowercase()
        if (applicationId != null && profile.associations.applicationIds.any {
                it.trim().lowercase() == applicationId
            }) {
            return true
        }
        val domain = normalizeDomain(webDomain) ?: return false
        return buildSet {
            addAll(profile.associations.domains)
            profile.associations.primaryUrl?.let(::add)
        }.any { normalizeDomain(it) == domain }
    }

    private fun normalizeDomain(value: String?): String? =
        value?.trim()?.lowercase()?.removePrefix("https://")?.removePrefix("http://")
            ?.substringBefore('/')?.substringBefore(':')?.removeSuffix(".")
            ?.takeIf(String::isNotBlank)

}
