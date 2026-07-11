package com.aozijx.passly.core.autofill.pipeline

import com.aozijx.passly.core.autofill.model.InternalFillRequest
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.domain.model.CredentialCandidate
import com.aozijx.passly.domain.repository.CredentialRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 候选项解析器：负责从 Repository 查询匹配的自动填充候选项。
 * 对查询异常做优雅降级，返回空列表。
 */
@Singleton
class CandidateResolver @Inject constructor(
    private val repository: CredentialRepository,
) {
    companion object {
        private const val TAG = "CandidateResolver"
    }

    fun resolve(request: InternalFillRequest): List<CredentialCandidate> {
        return try {
            repository.search(request.parentPackage, request.webDomain)
        } catch (e: Exception) {
            Logcat.e(TAG, "Candidate lookup failed for ${request.parentPackage}", e)
            emptyList()
        }
    }
}