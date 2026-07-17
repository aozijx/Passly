package com.aozijx.passly.domain.model.entry

import kotlinx.serialization.Serializable

/**
 * 网站/应用关联信息 —— 统一的领域对象。
 *
 * primaryUrl    用于：点击打开网页、编辑页面
 * matchDomains  专门用于：Autofill 域名匹配、Search 索引
 * packageNames  专门用于：Autofill 包名匹配
 */
@Serializable
data class WebsiteInfo(
    val primaryUrl: String? = null,
    val matchDomains: Set<String> = emptySet(),
    val packageNames: Set<String> = emptySet()
)
