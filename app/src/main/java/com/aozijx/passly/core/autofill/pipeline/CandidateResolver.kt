package com.aozijx.passly.core.autofill.pipeline

import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.autofill.model.InternalFillRequest
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.core.otp.OtpGenerator
import com.aozijx.passly.core.otp.OtpResult
import com.aozijx.passly.domain.autofill.policy.CredentialScopeMatcher
import com.aozijx.passly.domain.autofill.repository.CredentialServiceRepository
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.lookup.CredentialCandidate
import com.aozijx.passly.domain.settings.model.AutofillSettings
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
            !CredentialScopeMatcher.matches(selected, packageName, webDomain)
        ) {
            AppTelemetry.w(TAG, "Rejected selected credential outside request scope")
            return null
        }
        return selected.toResolvedCandidate(settings.includeOtp)
    }

    private fun CredentialCandidate.toResolved(includeOtp: Boolean): ResolvedCandidate {
        val entry = this.entry
        return ResolvedCandidate(
            candidateId = entry.id,
            displayName = entry.title,
            username = entry.username,
            password = entry.secret.login?.password ?: "",
            totpCode = if (includeOtp) generateTotpFromEntry(entry) else null,
            associatedDomain = entry.associatedDomain,
            associatedAppPackage = entry.associatedAppPackage,
            iconName = entry.iconName,
            iconCustomPath = entry.iconCustomPath,
            entryType = entry.entryType,
            matchedBy = matchedBy,
            matchedPackage = matchedPackage,
            matchedDomain = matchedDomain,
        )
    }

    private fun VaultEntry.toResolvedCandidate(includeOtp: Boolean): ResolvedCandidate {
        return ResolvedCandidate(
            candidateId = id,
            displayName = title,
            username = username,
            password = secret.login?.password ?: "",
            totpCode = if (includeOtp) generateTotpFromEntry(this) else null,
            associatedDomain = associatedDomain,
            associatedAppPackage = associatedAppPackage,
            iconName = iconName,
            iconCustomPath = iconCustomPath,
            entryType = entryType,
        )
    }

    private fun generateTotpFromEntry(entry: VaultEntry): String? {
        val otpConfig = entry.secret.otp?.config ?: return null
        if (otpConfig.secret.isBlank()) return null
        return when (val result = OtpGenerator.generate(otpConfig)) {
            is OtpResult.Success -> result.code
            is OtpResult.Failure -> null
        }
    }

}
