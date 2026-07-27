package com.aozijx.passly.core.autofill.matcher

import com.aozijx.passly.core.autofill.model.FieldRole
import com.aozijx.passly.core.autofill.model.InternalFillRequest

/**
 * 字段匹配策略接口。
 *
 * 接收已解析的字段列表，返回每个字段的角色分配。
 * 两个实现分别用于 CredentialProviderService(严格) 和 AutofillService(启发式)。
 *
 * 此接口严禁导入 android.service.autofill 或 android.service.credentials 包。
 */
interface FieldMatchStrategy {

    /**
     * @param request 解析后的填充请求（含所有可填充字段）
     * @return 字段到角色的映射。未被识别的字段不包含在结果中。
     */
    fun match(request: InternalFillRequest): MatchResult
}

data class MatchResult(
    /** key = FieldDescriptor.viewId, value = 分配的角色 */
    val roleMap: Map<String, FieldRole> = emptyMap(),
    /** 是否成功匹配到至少一个凭据字段 */
    val hasCredentials: Boolean = false,
)