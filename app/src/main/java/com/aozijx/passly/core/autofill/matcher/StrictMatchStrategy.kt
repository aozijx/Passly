package com.aozijx.passly.core.autofill.matcher

import com.aozijx.passly.core.autofill.model.FieldDescriptor
import com.aozijx.passly.core.autofill.model.FieldRole
import com.aozijx.passly.core.autofill.model.InternalFillRequest
import javax.inject.Inject

/**
 * 严格匹配策略：供 CredentialProviderService 使用。
 *
 * 仅匹配 autofillHints 中的标准常量。
 * 作为降级方案：若无 Hint，尝试通过 ViewId 后缀做简单正则匹配。
 *
 * 标准 hints 参考 androidx.credentials 中 Constants 的定义：
 * - PASSWORD_AUTOFILL_HINT → PASSWORD
 * - USERNAME_AUTOFILL_HINT → USERNAME
 * - 短信验证码 / OTP → SMS_OTP、ONE_TIME_CODE
 */
class StrictMatchStrategy @Inject constructor() : FieldMatchStrategy {

    companion object {
        /** 标准 hint → FieldRole 映射 */
        private val HINT_ROLE_MAP = mapOf(
            "PASSWORD" to FieldRole.PASSWORD,
            "USERNAME" to FieldRole.USERNAME,
            "EMAIL_ADDRESS" to FieldRole.USERNAME,
            "SMS_OTP" to FieldRole.OTP,
            "ONE_TIME_CODE" to FieldRole.OTP,
        )

        /** ViewId 后缀降级匹配正则 */
        private val RESOURCE_ID_PATTERNS = listOf(
            Regex("(?i)(pass(word|wd|code))|pwd", RegexOption.IGNORE_CASE) to FieldRole.PASSWORD,
            Regex(
                "(?i)(user(name)?|email|account|login)",
                RegexOption.IGNORE_CASE
            ) to FieldRole.USERNAME,
            Regex(
                "(?i)(otp|sms|code|verify|token|verification)",
                RegexOption.IGNORE_CASE
            ) to FieldRole.OTP,
        )

        /** Submit 按钮识别 */
        private val SUBMIT_PATTERNS = listOf(
            Regex(
                "(?i)(submit|login|sign[_\\-]?in|next|continue|go|ok|done)",
                RegexOption.IGNORE_CASE
            ) to FieldRole.SUBMIT,
        )
    }

    override fun match(request: InternalFillRequest): MatchResult {
        val roleMap = mutableMapOf<String, FieldRole>()

        for (field in request.fields) {
            val role = matchField(field)
            if (role != FieldRole.UNKNOWN) {
                roleMap[field.viewId] = role
            }
        }

        val hasCredentials =
            roleMap.values.any { it == FieldRole.USERNAME || it == FieldRole.PASSWORD }
        return MatchResult(roleMap = roleMap, hasCredentials = hasCredentials)
    }

    private fun matchField(field: FieldDescriptor): FieldRole {
        // 第一优先级：autofill hints
        for (hint in field.autofillHints) {
            HINT_ROLE_MAP[hint.uppercase()]?.let { return it }
        }

        // 第二优先级：resourceId 后缀匹配（降级方案）
        val rawId = field.resourceId ?: return FieldRole.UNKNOWN
        // 提取 ID 最后一部分（格式：@id/password_field → password_field）
        val shortId = rawId.substringAfterLast("/", rawId)

        for ((pattern, role) in RESOURCE_ID_PATTERNS) {
            if (pattern.containsMatchIn(shortId)) {
                return role
            }
        }

        // Submit 按钮
        val cls = field.className?.lowercase() ?: ""
        val isButtonlike =
            cls.contains("button") || cls.contains("imageview") || cls.contains("textview")
        if (isButtonlike) {
            for ((pattern, role) in SUBMIT_PATTERNS) {
                if (pattern.containsMatchIn(shortId)) return role
            }
            // 检查 hint text
            val hintText = field.hint ?: field.contentDescription ?: ""
            if (hintText.isNotBlank()) {
                for ((pattern, role) in SUBMIT_PATTERNS) {
                    if (pattern.containsMatchIn(hintText)) return role
                }
            }
        }

        return FieldRole.UNKNOWN
    }
}