package com.aozijx.passly.core.autofill.matcher

import com.aozijx.passly.core.autofill.model.FieldRole
import com.aozijx.passly.core.autofill.model.InternalFillRequest

/**
 * 字段匹配策略接口。
 *
 * 接收已解析的字段列表，返回字段角色分配与表单触发信息。
 * 两个实现分别用于 CredentialProviderService(严格) 和 AutofillService(启发式)。
 *
 * 触发判定与角色识别分离：
 * - [hasEditableFields]：页面是否存在可编辑输入框。决定自动填充是否"触发"——
 *   样式化/自定义控件即使识别不出具体角色，只要有输入框就应给出填充入口。
 * - [roleMap]/[hasCredentials]：能猜出的字段角色。决定候选填充时如何映射字段。
 *   识别不出的字段不进入映射，由填充阶段按字段顺序兜底。
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
    /** 是否成功匹配到至少一个凭据字段（username/password） */
    val hasCredentials: Boolean = false,
    /** 页面是否存在可编辑输入框（触发自动填充的充分条件） */
    val hasEditableFields: Boolean = false,
)
