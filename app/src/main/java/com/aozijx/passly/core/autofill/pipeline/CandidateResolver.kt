package com.aozijx.passly.core.autofill.pipeline

import com.aozijx.passly.core.autofill.model.InternalFillRequest
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.core.otp.TwoFAUtils
import com.aozijx.passly.domain.model.CredentialCandidate
import com.aozijx.passly.domain.model.MatchType
import com.aozijx.passly.domain.repository.CredentialRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 候选项解析器：从 Repository 查询匹配的凭据，转换为 UI 安全的 [ResolvedCandidate]。
 *
 * 此组件是 VaultEntry -> ResolvedCandidate 的唯一转换点，
 * 下游（BottomSheet、ResponseFactory、Legacy/Modern Factory）只接收 ResolvedCandidate，
 * 遵循最小暴露原则（Principle of Least Exposure）。
 */
@Singleton
class CandidateResolver @Inject constructor(
    private val repository: CredentialRepository,
) {
    companion object {
        private const val TAG = "CandidateResolver"
    }

    /**
     * 根据 [InternalFillRequest] 解析候选凭据。
     * 由 [FillRequestDispatcher] 调用。
     */
    fun resolve(request: InternalFillRequest): List<ResolvedCandidate> {
        return resolveByPackage(request.parentPackage, request.webDomain)
    }

    /**
     * 按包名/域名搜索并转换为 [ResolvedCandidate]。
     * 由 AutofillFillViewModel 的 UnlockOnly 路径调用。
     */
    fun resolveByPackage(
        packageName: String?,
        webDomain: String?,
    ): List<ResolvedCandidate> {
        return try {
            repository.search(packageName, webDomain).map { it.toResolved() }
        } catch (e: Exception) {
            Logcat.e(TAG, "Candidate lookup failed for $packageName", e)
            emptyList()
        }
    }

    /**
     * 按 ID 列表批量解析候选凭据。
     * 由 AutofillFillViewModel 的 BottomSheet 直接查阅路径调用。
     */
    fun resolveByIds(ids: List<Int>): List<ResolvedCandidate> {
        return try {
            repository.getByIds(ids).map { entry ->
                ResolvedCandidate(
                    candidateId = entry.id,
                    displayName = entry.title,
                    username = entry.username,
                    password = entry.password,
                    totpCode = TwoFAUtils.generateCurrentTotpFromEntry(entry),
                    associatedDomain = entry.associatedDomain,
                    associatedAppPackage = entry.associatedAppPackage,
                    subtitle = entry.username,
                    iconName = entry.iconName,
                    iconCustomPath = entry.iconCustomPath,
                    entryType = entry.entryType,
                )
            }
        } catch (e: Exception) {
            Logcat.e(TAG, "resolveByIds failed", e)
            emptyList()
        }
    }

    // -- 内部转换 --

    private fun CredentialCandidate.toResolved(): ResolvedCandidate {
        val entry = this.entry
        return ResolvedCandidate(
            candidateId = entry.id,
            displayName = entry.title,
            username = entry.username,
            password = entry.password,
            totpCode = TwoFAUtils.generateCurrentTotpFromEntry(entry),
            associatedDomain = entry.associatedDomain,
            associatedAppPackage = entry.associatedAppPackage,
            subtitle = buildSubtitle(this),
            iconName = entry.iconName,
            iconCustomPath = entry.iconCustomPath,
            entryType = entry.entryType,
            matchedBy = matchedBy,
            matchedPackage = matchedPackage,
            matchedDomain = matchedDomain,
        )
    }

    private fun buildSubtitle(candidate: CredentialCandidate): String {
        val parts = mutableListOf<String>()
        when (candidate.matchedBy) {
            MatchType.PACKAGE_NAME -> candidate.matchedPackage?.let { parts.add(it) }
            MatchType.WEB_DOMAIN -> candidate.matchedDomain?.let { parts.add(it) }
            else -> {}
        }
        if (candidate.entry.totpSecret?.isNotBlank() == true) {
            parts.add("2FA")
        }
        return parts.joinToString(" · ")
    }
}
