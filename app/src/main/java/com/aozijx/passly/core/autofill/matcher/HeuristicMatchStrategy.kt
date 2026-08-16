package com.aozijx.passly.core.autofill.matcher

import com.aozijx.passly.core.autofill.model.FieldDescriptor
import com.aozijx.passly.core.autofill.model.FieldRole
import com.aozijx.passly.core.autofill.model.InternalFillRequest
import javax.inject.Inject

/**
 * 启发式匹配策略：供 AutofillService 使用。
 *
 * 触发判定与角色识别分离（根因修复）：
 * - [hasEditableFields]：页面有可编辑文本输入框即触发填充入口，
 *   不依赖能否猜出 username/password —— 样式化/自定义控件不再被漏掉。
 * - [roleMap]：尽力识别字段角色，供填充阶段映射（hints → inputType → id → hint 文本）。
 */
class HeuristicMatchStrategy @Inject constructor() : FieldMatchStrategy {

    companion object {
        // Hint 到角色的映射（宽松版，包含更多变体）
        private val HINT_PRIORITY = mapOf(
            "PASSWORD" to FieldRole.PASSWORD,
            "NEW_PASSWORD" to FieldRole.PASSWORD,
            "CONFIRM_PASSWORD" to FieldRole.PASSWORD,
            "USERNAME" to FieldRole.USERNAME,
            "EMAIL_ADDRESS" to FieldRole.USERNAME,
            "SMS_OTP" to FieldRole.OTP,
            "ONE_TIME_CODE" to FieldRole.OTP,
            "PHONE" to FieldRole.USERNAME,
        )

        // inputType 字符串模式 pwd
        private val PASSWORD_INPUT_TYPES = setOf(
            "TEXT_VARIATION_PASSWORD",
            "TEXT_VARIATION_VISIBLE_PASSWORD",
            "TEXT_VARIATION_WEB_PASSWORD",
            "NUMBER_VARIATION_PASSWORD",
        )

        // inputType 字符串模式 username
        private val USERNAME_INPUT_TYPES = setOf(
            "TEXT_VARIATION_EMAIL_ADDRESS",
            "TEXT_VARIATION_PERSON_NAME",
            "TEXT_VARIATION_WEB_EMAIL_ADDRESS",
        )

        // View 名称关键字（宽松）
        private val USERNAME_VIEW_ID_PATTERNS = Regex(
            "(?i)(user[_\\-]?name|email|account|login|id$|phonenumber|phone[_\\-]?number)",
            RegexOption.IGNORE_CASE
        )
        private val PASSWORD_VIEW_ID_PATTERNS = Regex(
            "(?i)(password|passwd|pwd$|pass$)",
            RegexOption.IGNORE_CASE
        )
        private val OTP_VIEW_ID_PATTERNS = Regex(
            "(?i)(otp|sms|pin|code|verify|token|verification|mfa|2fa|captcha)",
            RegexOption.IGNORE_CASE
        )

        // hint 文本关键字
        private val USERNAME_HINT_WORDS = Regex(
            "(?i)(username|email|phone|mobile|account|id|login|手机号|邮箱|账号|用户名|号码)",
            RegexOption.IGNORE_CASE
        )
        private val PASSWORD_HINT_WORDS = Regex(
            "(?i)(password|passcode|passphrase|密钥|密码|口令)",
            RegexOption.IGNORE_CASE
        )
        private val OTP_HINT_WORDS = Regex(
            "(?i)(otp|sms|code|pin|verify|verification|token|captcha|验证码|校验码|一次性|动态)",
            RegexOption.IGNORE_CASE
        )
        private val SUBMIT_HINT_WORDS = Regex(
            "(?i)(submit|login|sign[_\\s-]?in|next|continue|go|done|ok|登录|提交|确认|下一步|继续)",
            RegexOption.IGNORE_CASE
        )
    }

    override fun match(request: InternalFillRequest): MatchResult {
        val roleMap = mutableMapOf<String, FieldRole>()

        // 第一轮：识别凭据字段角色
        for (field in request.fields) {
            val role = matchFieldHeuristic(field)
            if (role != FieldRole.UNKNOWN) {
                roleMap[field.viewId] = role
            }
        }

        // 触发判定独立于角色识别：任何可编辑文本输入框都构成可填充表单。
        val hasEditableFields = request.fields.any { it.isEditableTextInput() }
        val hasCredentials =
            roleMap.values.any { it == FieldRole.USERNAME || it == FieldRole.PASSWORD }

        return MatchResult(
            roleMap = roleMap,
            hasCredentials = hasCredentials,
            hasEditableFields = hasEditableFields,
        )
    }

    private fun matchFieldHeuristic(field: FieldDescriptor): FieldRole {
        // 优先级 1: autofill hints
        for (hint in field.autofillHints) {
            HINT_PRIORITY[hint.uppercase()]?.let { return it }
        }

        val inputType = field.inputType ?: ""
        val rawId = (field.resourceId ?: "").substringAfterLast("/", "")
        val hint = field.hint ?: ""
        val contentDesc = field.contentDescription ?: ""
        val combinedHint = "$hint $contentDesc"
        val cls = field.className?.lowercase() ?: ""

        // 优先级 2: inputType
        for (pwdType in PASSWORD_INPUT_TYPES) {
            if (pwdType in inputType) return FieldRole.PASSWORD
        }
        for (userType in USERNAME_INPUT_TYPES) {
            if (userType in inputType) return FieldRole.USERNAME
        }

        // 优先级 3: resourceId
        if (PASSWORD_VIEW_ID_PATTERNS.containsMatchIn(rawId)) return FieldRole.PASSWORD
        if (OTP_VIEW_ID_PATTERNS.containsMatchIn(rawId) && USERNAME_VIEW_ID_PATTERNS.containsMatchIn(
                rawId
            )
        ) {
            // 同时匹配 userName 和 otp → 可能是账户验证码，偏向 userName
            // 具体判断：优先 hint 文本
            if (OTP_HINT_WORDS.containsMatchIn(combinedHint)) return FieldRole.OTP
            return FieldRole.USERNAME
        }
        if (OTP_VIEW_ID_PATTERNS.containsMatchIn(rawId)) return FieldRole.OTP
        if (USERNAME_VIEW_ID_PATTERNS.containsMatchIn(rawId)) return FieldRole.USERNAME

        // 优先级 4: hint/contentDescription 文本
        val isButtonlike =
            cls.contains("button") || cls.contains("imageview") || cls.contains("textview")
        if (!isButtonlike) {
            // 普通输入框：按 hint 文本推断
            if (USERNAME_HINT_WORDS.containsMatchIn(combinedHint)) return FieldRole.USERNAME
            if (PASSWORD_HINT_WORDS.containsMatchIn(combinedHint)) return FieldRole.PASSWORD
            if (OTP_HINT_WORDS.containsMatchIn(combinedHint)) return FieldRole.OTP
        } else {
            // 按钮类：匹配提交语义
            if (SUBMIT_HINT_WORDS.containsMatchIn(combinedHint) || SUBMIT_HINT_WORDS.containsMatchIn(
                    rawId
                )
            ) {
                return FieldRole.SUBMIT
            }
        }

        return FieldRole.UNKNOWN
    }

    /** 判断字段是否为可编辑文本输入框（含样式化自定义控件）。 */
    private fun FieldDescriptor.isEditableTextInput(): Boolean {
        if (className?.contains("edittext", ignoreCase = true) == true) return true
        if (className?.contains("textinput", ignoreCase = true) == true) return true
        if (autofillHints.isNotEmpty()) return true
        // inputType 字符串包含文本/数字/邮箱/电话类
        return inputType?.let { raw ->
            raw.contains("TYPE_CLASS_TEXT") || raw.contains("TYPE_CLASS_NUMBER") ||
                raw.contains("TYPE_CLASS_PHONE") || raw.contains("TYPE_CLASS_DATETIME")
        } ?: false
    }
}
