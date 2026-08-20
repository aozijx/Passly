package com.aozijx.passly.domain.autofill.port

import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.FieldRole

/**
 * 字段匹配策略接口。
 */
interface FieldMatchStrategy {

    /**
     * @param request 解析后的填充请求
     * @return 字段到角色的映射。
     */
    fun match(request: AutofillRequest): MatchResult
}

data class MatchResult(
    /** key = AutofillField.id, value = 分配的角色 */
    val roleMap: Map<String, FieldRole> = emptyMap(),
    /** 是否识别出至少一个凭据字段，作为触发条件 */
    val hasCredentials: Boolean = false,
)
