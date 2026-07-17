package com.aozijx.passly.domain.model.lookup

import com.aozijx.passly.domain.model.entry.VaultEntry

enum class LookupField {
    TITLE,
    USERNAME,
    EMAIL,
    DOMAIN,
    URL,
    PACKAGE
}

data class LookupFieldValue(
    val field: LookupField,
    val text: String
)

enum class MatchType(val score: Int) {
    PACKAGE_NAME(100),
    DIGITAL_ASSET_LINK(90),
    WEB_DOMAIN(80),
    TITLE(60),
    URL(40),
    UNKNOWN(0)
}

/**
 * 凭据候选项：Credential / Autofill 两条路径共用。
 *
 * 封装搜索返回的凭据条目和匹配元数据。
 * Legacy 路径通过适配层转换为 Dataset，Modern 路径转换为 CredentialEntry。
 */
data class CredentialCandidate(
    val entry: VaultEntry,
    /** 匹配分数，等于 [matchedBy.score] */
    val score: Int,
    /** 通过何种方式匹配到该条目 */
    val matchedBy: MatchType,
    /** 匹配到的包名（来自 request 或 entry） */
    val matchedPackage: String? = null,
    /** 匹配到的域名（来自 request 或 entry） */
    val matchedDomain: String? = null,
)
