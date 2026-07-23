package com.aozijx.passly.core.autofill.pipeline

import com.aozijx.passly.core.autofill.model.InternalFillRequest
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.otp.OtpGenerator
import com.aozijx.passly.core.otp.OtpResult
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.lookup.CredentialCandidate
import com.aozijx.passly.domain.model.lookup.MatchType
import com.aozijx.passly.domain.repository.autofill.CredentialServiceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CandidateResolver @Inject constructor(
    private val repository: CredentialServiceRepository,
) {
    companion object {
        private const val TAG = "CandidateResolver"
    }

    fun resolve(request: InternalFillRequest): List<ResolvedCandidate> {
        return resolveByPackage(request.parentPackage, request.webDomain)
    }

    fun resolveByPackage(
        packageName: String?,
        webDomain: String?,
    ): List<ResolvedCandidate> {
        return try {
            repository.search(packageName, webDomain).map { it.toResolved() }
        } catch (e: Exception) {
            AppLog.e(TAG, "Candidate lookup failed for $packageName", e)
            emptyList()
        }
    }

    fun resolveByIds(ids: List<Int>): List<ResolvedCandidate> {
        return try {
            repository.getByIds(ids).map { entry ->
                entry.toResolvedCandidate()
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "resolveByIds failed", e)
            emptyList()
        }
    }

    private fun CredentialCandidate.toResolved(): ResolvedCandidate {
        val entry = this.entry
        return ResolvedCandidate(
            candidateId = entry.id.toIntOrNull() ?: 0,
            displayName = entry.title,
            username = entry.username,
            password = entry.secret.login?.password ?: "",
            totpCode = generateTotpFromEntry(entry),
            associatedDomain = entry.associatedDomain,
            associatedAppPackage = entry.associatedAppPackage,
            subtitle = buildSubtitle(this),
            iconName = entry.iconName,
            iconCustomPath = entry.iconCustomPath,
            entryType = entry.entryType.name,
            matchedBy = matchedBy,
            matchedPackage = matchedPackage,
            matchedDomain = matchedDomain,
        )
    }

    private fun VaultEntry.toResolvedCandidate(): ResolvedCandidate {
        return ResolvedCandidate(
            candidateId = id.toIntOrNull() ?: 0,
            displayName = title,
            username = username,
            password = secret?.login?.password ?: "",
            totpCode = generateTotpFromEntry(this),
            associatedDomain = associatedDomain,
            associatedAppPackage = associatedAppPackage,
            subtitle = username,
            iconName = iconName,
            iconCustomPath = iconCustomPath,
            entryType = entryType.name,
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

    private fun buildSubtitle(candidate: CredentialCandidate): String {
        val parts = mutableListOf<String>()
        when (candidate.matchedBy) {
            MatchType.PACKAGE_NAME -> candidate.matchedPackage?.let { parts.add(it) }
            MatchType.WEB_DOMAIN -> candidate.matchedDomain?.let { parts.add(it) }
            else -> {}
        }
        if (candidate.entry.secret.otp?.config?.secret?.isNotBlank() == true) {
            parts.add("2FA")
        }
        return parts.joinToString(" · ")
    }
}
