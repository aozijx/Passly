package com.aozijx.passly.domain.repository

import com.aozijx.passly.domain.model.CredentialCandidate
import com.aozijx.passly.domain.model.VaultEntry

/**
 * 凭据仓库接口（domain 层）。
 * 提供通用凭据的查询、解密和持久化操作，不绑定特定自动填充系统。
 *
 * Autofill / Credential Manager / Passkey 等场景统一通过此接口访问凭据数据。
 */
interface CredentialRepository {

    /** 按包名/域名搜索匹配的凭据候选项 */
    fun search(
        packageName: String?,
        webDomain: String?
    ): List<CredentialCandidate>

    /** 按 ID 获取单个凭据条目 */
    fun getById(entryId: Int): VaultEntry?

    /** 按 ID 列表批量获取凭据条目 */
    fun getByIds(entryIds: List<Int>): List<VaultEntry>

    /** 解密凭据的敏感字段（password / totpSecret 等） */
    fun decrypt(entry: VaultEntry): VaultEntry?

    /** 更新凭据的最后使用时间 */
    fun updateLastUsed(entry: VaultEntry)

    /** 保存或更新凭据条目 */
    fun save(
        packageName: String?,
        webDomain: String?,
        pageTitle: String?,
        usernameValue: String,
        passwordValue: String
    ): Boolean
}
