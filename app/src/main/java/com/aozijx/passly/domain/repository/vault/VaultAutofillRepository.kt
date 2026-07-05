package com.aozijx.passly.domain.repository.vault

import com.aozijx.passly.domain.model.AutofillCandidate
import com.aozijx.passly.domain.model.VaultEntry

/**
 * Vault 仓库在 Autofill 场景下的能力子集。
 *
 * 之所以从 [VaultRepository] 中拆出独立接口，是因为 Autofill 服务进程需要的能力面
 * 与主 Vault 页面不同：它需要"按包名/域名匹配候选条目"以及"保存或更新自动填充来源的条目"，
 * 这些操作的语义、调用方（系统 Autofill 服务 / AutofillAuthActivity）与主页面隔离。
 *
 * 该接口不暴露任何加解密/UI 相关概念，只承载业务操作。
 */
interface VaultAutofillRepository {
    suspend fun updateUsageStats(entry: VaultEntry)
    suspend fun getEntryById(entryId: Int): VaultEntry?
    suspend fun getEntriesByIds(entryIds: List<Int>): List<VaultEntry>
    suspend fun findMatchingCandidates(
        packageName: String?,
        webDomain: String?
    ): List<AutofillCandidate>

    suspend fun saveOrUpdateEntry(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): Boolean
}