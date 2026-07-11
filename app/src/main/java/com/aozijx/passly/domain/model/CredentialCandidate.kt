package com.aozijx.passly.domain.model

/**
 * 匹配类型及对应分数。
 * score 越高匹配越精确。排序时按 score 降序。
 *
 * 新增匹配策略只需在此枚举中添加，Dispatcher 无需修改。
 */
enum class MatchType(val score: Int) {
    /** 包名精确匹配 */
    PACKAGE_NAME(100),

    /** Digital Asset Link 验证通过（预留） */
    DIGITAL_ASSET_LINK(90),

    /** 域名匹配 */
    WEB_DOMAIN(80),

    /** 页面标题匹配（预留） */
    TITLE(60),

    /** URL 匹配（预留） */
    URL(40),

    /** 无匹配 */
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