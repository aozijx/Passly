package com.aozijx.passly.core.autofill.matcher

import com.aozijx.passly.core.autofill.model.FieldRole
import com.aozijx.passly.core.autofill.model.InternalFillRequest

/**
 * 字段匹配策略接口。
 *
 * 接收已解析的字段列表，返回字段角色分配与表单触发信息。
 * 两个实现分别用于 CredentialProviderService(严格) 和 AutofillService(启发式)。
 *
 * 触发语义：
 * - [hasCredentials]：识别出至少一个凭据字段（username/password/otp）→ 触发填充入口。
 *   普通输入框（搜索框、单输入表单）识别不出凭据角色 → 不触发。
 * - [roleMap]：字段到角色的映射，供填充阶段映射。识别不出的字段不进入映射。
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
    /** 是否识别出至少一个凭据字段（username/password/otp），作为触发条件 */
    val hasCredentials: Boolean = false,
)
