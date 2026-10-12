package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSource
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.autofill.port.FieldMatchStrategy
import com.aozijx.passly.domain.autofill.port.MatchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillSaveDraftExtractorTest {
    @Test
    fun `extracts username and password from classified fields`() {
        val extractor = AutofillSaveDraftExtractor(strategy(username = "user", password = "pass"))

        val result = extractor(request("alice@example.com", "correct horse"))

        assertEquals(
            AutofillSaveDraftExtraction.Ready(
                draft = com.aozijx.passly.domain.autofill.model.AutofillSaveDraft(
                    packageName = "com.example.app",
                    webDomain = "example.com",
                    pageTitle = "Sign in",
                    username = "alice@example.com",
                    password = "correct horse",
                ),
            ),
            result,
        )
    }

    @Test
    fun `accepts password only forms with an explicit blank username`() {
        val extractor = AutofillSaveDraftExtractor(strategy(password = "pass"))

        val result = extractor(request(username = null, password = "secret"))

        assertEquals(
            "",
            (result as AutofillSaveDraftExtraction.Ready).draft.username,
        )
        assertEquals("secret", result.draft.password)
    }

    @Test
    fun `rejects a request without a classified password value`() {
        val extractor = AutofillSaveDraftExtractor(strategy(username = "user"))

        val result = extractor(request(username = "alice", password = null))

        assertTrue(result is AutofillSaveDraftExtraction.Rejected)
        assertEquals(
            AutofillSaveDraftRejection.PASSWORD_MISSING,
            (result as AutofillSaveDraftExtraction.Rejected).reason,
        )
    }

    private fun request(username: String?, password: String?): AutofillRequest = AutofillRequest(
        packageName = "com.example.app",
        domain = "example.com",
        fields = buildList {
            username?.let { add(field("user", it)) }
            password?.let { add(field("pass", it)) }
        },
        source = AutofillSource.AUTOFILL_SERVICE,
        activityTitle = "Sign in",
    )

    private fun field(id: String, value: String) = AutofillField(
        id = id,
        hints = emptySet(),
        inputType = null,
        isFocused = false,
        text = value,
    )

    private fun strategy(
        username: String? = null,
        password: String? = null,
    ): FieldMatchStrategy = object : FieldMatchStrategy {
        override fun match(request: AutofillRequest): MatchResult = MatchResult(
            roleMap = buildMap {
                username?.let { put(it, FieldRole.USERNAME) }
                password?.let { put(it, FieldRole.PASSWORD) }
            },
            hasCredentials = password != null,
        )
    }
}
