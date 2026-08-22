package com.aozijx.passly.feature.autofill.internal

import com.aozijx.passly.domain.autofill.port.AutofillHintProvider
import com.aozijx.passly.domain.autofill.port.MatchResult
import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSource
import com.aozijx.passly.domain.autofill.model.FieldRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeuristicMatchStrategyTest {

    private val testHintProvider = object : AutofillHintProvider {
        private fun getUsernameKeywords() = listOf("username", "email", "account", "login", "id", "手机号", "邮箱", "账号", "用户名")
        private fun getPasswordKeywords() = listOf("password", "passcode", "passphrase", "passwd", "pwd", "密码", "口令")
        private fun getOtpKeywords() = listOf("otp", "sms", "token", "verify", "verification", "captcha", "验证码")
        private fun getSubmitKeywords() = listOf("submit", "login", "signin", "登录", "确认")
        private fun getSearchKeywords() = listOf("search", "搜索")

        override fun getUsernamePattern() = buildRegex(getUsernameKeywords())
        override fun getPasswordPattern() = buildRegex(getPasswordKeywords())
        override fun getOtpPattern() = buildRegex(getOtpKeywords())
        override fun getSubmitPattern() = buildRegex(getSubmitKeywords())
        override fun getSearchPattern() = buildRegex(getSearchKeywords())
        override fun getConfirmationPattern() = Regex("(?!)")

        override fun getHintRoleMap(): Map<String, FieldRole> = mapOf(
            "emailaddress" to FieldRole.USERNAME,
            "password" to FieldRole.PASSWORD,
            "onetimecode" to FieldRole.OTP
        )

        private fun buildRegex(keywords: List<String>): Regex {
            val pattern = keywords.joinToString("|") { Regex.escape(it) }
            return Regex("(?i)($pattern)", RegexOption.IGNORE_CASE)
        }
    }

    private val matcher = HeuristicMatchStrategy(testHintProvider)

    private fun match(vararg fields: AutofillField): MatchResult = matcher.match(
        AutofillRequest(
            packageName = "com.example",
            domain = null,
            fields = fields.toList(),
            source = AutofillSource.AUTOFILL_SERVICE,
        )
    )

    @Test
    fun `camelCase standard hints are normalized before matching`() {
        val result = match(
            FieldDescriptor("id/email", autofillHints = listOf("emailAddress")),
            FieldDescriptor("id/pwd", autofillHints = listOf("password")),
        )
        assertTrue(result.hasCredentials)
        assertEquals(FieldRole.USERNAME, result.roleMap["id/email"])
        assertEquals(FieldRole.PASSWORD, result.roleMap["id/pwd"])
    }

    @Test
    fun `oneTimeCode hint maps to OTP`() {
        val result = match(
            FieldDescriptor("id/otp", autofillHints = listOf("oneTimeCode")),
        )
        assertTrue(result.hasCredentials)
        assertEquals(FieldRole.OTP, result.roleMap["id/otp"])
    }

    @Test
    fun `web edit text inputType is a weak username signal`() {
        val result = match(
            FieldDescriptor(
                "id/et_1",
                inputType = "TYPE_CLASS_TEXT TEXT_VARIATION_WEB_EDIT_TEXT",
            ),
        )
        assertTrue(result.hasCredentials)
        assertEquals(FieldRole.USERNAME, result.roleMap["id/et_1"])
    }

    @Test
    fun `plain text field without login signal does not trigger`() {
        val result = match(
            FieldDescriptor("id/et_1", inputType = "TYPE_CLASS_TEXT"),
        )
        assertFalse(result.hasCredentials)
    }

    @Test
    fun `password term in id triggers even without standard inputType`() {
        val result = match(
            FieldDescriptor("id/et_1", resourceId = "@id/login_passcode", inputType = "TYPE_CLASS_TEXT"),
        )
        assertTrue(result.hasCredentials)
        assertEquals(FieldRole.PASSWORD, result.roleMap["id/et_1"])
    }

    @Test
    fun `chinese password hint triggers`() {
        val result = match(
            FieldDescriptor("id/et_1", hint = "请输入密码"),
        )
        assertTrue(result.hasCredentials)
        assertEquals(FieldRole.PASSWORD, result.roleMap["id/et_1"])
    }

    @Test
    fun `search field with user-like id is excluded`() {
        val result = match(
            FieldDescriptor("id/search_user", resourceId = "@id/search_user", inputType = "TYPE_CLASS_TEXT"),
        )
        assertFalse(result.hasCredentials)
    }

    @Test
    fun `search resource id without slash is excluded`() {
        val result = match(
            FieldDescriptor(
                "search_user",
                resourceId = "search_user",
                inputType = "TYPE_CLASS_TEXT",
            ),
        )

        assertFalse(result.hasCredentials)
    }

    @Test
    fun `confirmation resource id without slash is not classified as password`() {
        val matcher = HeuristicMatchStrategy(object : AutofillHintProvider by testHintProvider {
            override fun getConfirmationPattern() = Regex("confirm", RegexOption.IGNORE_CASE)
        })
        val result = matcher.match(
            AutofillRequest(
                packageName = "com.example",
                domain = null,
                fields = listOf(
                    FieldDescriptor(
                        "confirm_password",
                        resourceId = "confirm_password",
                        inputType = "TYPE_CLASS_TEXT TEXT_VARIATION_PASSWORD",
                    ),
                ),
                source = AutofillSource.AUTOFILL_SERVICE,
            )
        )

        assertFalse(result.hasCredentials)
    }

    @Test
    fun `otp regex does not match postcode or country code`() {
        val result = match(
            FieldDescriptor("id/postcode", resourceId = "@id/postcode", inputType = "TYPE_CLASS_NUMBER"),
            FieldDescriptor("id/country_code", resourceId = "@id/country_code", inputType = "TYPE_CLASS_NUMBER"),
        )
        assertFalse(result.hasCredentials)
    }

    @Test
    fun `otp combined pattern still matches verification code`() {
        val result = match(
            FieldDescriptor("id/verify_code", resourceId = "@id/verify_code", inputType = "TYPE_CLASS_NUMBER"),
        )
        assertTrue(result.hasCredentials)
        assertEquals(FieldRole.OTP, result.roleMap["id/verify_code"])
    }

    @Test
    fun `focused editable field is synthesized as username in login context`() {
        val result = match(
            FieldDescriptor("id/password", resourceId = "@id/password", inputType = "TYPE_CLASS_TEXT TEXT_VARIATION_PASSWORD"),
            FieldDescriptor("id/et_focused", resourceId = "@id/et_focused", inputType = "TYPE_CLASS_TEXT", isFocused = true),
        )
        assertTrue(result.hasCredentials)
        assertEquals(FieldRole.USERNAME, result.roleMap["id/et_focused"])
    }

    @Test
    fun `focused field is not synthesized without login context`() {
        val result = match(
            FieldDescriptor("field1", resourceId = "field1", inputType = "TYPE_CLASS_TEXT", isFocused = true),
        )
        assertFalse(result.hasCredentials)
    }

    @Test
    fun `login term plus focused field triggers weak request`() {
        val result = match(
            FieldDescriptor("id/et_focused", resourceId = "@id/et_focused", inputType = "TYPE_CLASS_TEXT", isFocused = true),
            FieldDescriptor("id/login_hint", resourceId = "@id/account_field", inputType = "TYPE_CLASS_TEXT"),
        )
        assertTrue(result.hasCredentials)
    }

    @Test
    fun `password-only form triggers and password role is assigned`() {
        val result = match(
            FieldDescriptor("id/et_1", inputType = "TYPE_CLASS_TEXT TEXT_VARIATION_PASSWORD"),
        )
        assertTrue(result.hasCredentials)
        assertEquals(FieldRole.PASSWORD, result.roleMap["id/et_1"])
    }
}

private fun FieldDescriptor(
    viewId: String,
    autofillHints: List<String> = emptyList(),
    inputType: String? = null,
    isFocused: Boolean = false,
    resourceId: String? = null,
    hint: String? = null,
    contentDescription: String? = null,
    className: String? = null,
): AutofillField = AutofillField(
    id = viewId,
    hints = autofillHints.toSet(),
    inputType = inputType,
    isFocused = isFocused,
    resourceId = resourceId,
    hint = hint,
    contentDescription = contentDescription,
    className = className,
)
