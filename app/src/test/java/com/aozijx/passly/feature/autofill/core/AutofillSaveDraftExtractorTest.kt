package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSource
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.autofill.port.FieldMatchStrategy
import com.aozijx.passly.domain.autofill.port.MatchResult
import org.junit.Assert.assertEquals
import org.junit.Test

class AutofillSaveDraftExtractorTest {
    @Test
    fun `classified values produce a title-free legacy save draft`() {
        val extractor = AutofillSaveDraftExtractor(strategy("user" to FieldRole.USERNAME, "pass" to FieldRole.PASSWORD))

        val result = extractor(request(packageName = " com.example.app ", username = "alice", password = "secret"))

        val draft = (result as AutofillSaveDraftExtraction.Ready).draft
        assertEquals("com.example.app", draft.packageName)
        assertEquals("example.com", draft.webDomain)
        assertEquals(null, draft.pageTitle)
        assertEquals("alice", draft.username)
        assertEquals("secret", draft.password)
    }

    @Test
    fun `password only form keeps an explicit blank username`() {
        val extractor = AutofillSaveDraftExtractor(strategy("pass" to FieldRole.PASSWORD))

        val result = extractor(request(username = null, password = "secret"))

        assertEquals("", (result as AutofillSaveDraftExtraction.Ready).draft.username)
    }

    @Test
    fun `missing package is rejected`() {
        val extractor = AutofillSaveDraftExtractor(strategy("pass" to FieldRole.PASSWORD))

        val result = extractor(request(packageName = " ", password = "secret"))

        assertEquals(
            AutofillSaveDraftExtraction.Rejected(AutofillSaveDraftRejection.PACKAGE_MISSING),
            result,
        )
    }

    @Test
    fun `blank classified password is rejected`() {
        val extractor = AutofillSaveDraftExtractor(strategy("pass" to FieldRole.PASSWORD))

        val result = extractor(request(password = "   "))

        assertEquals(
            AutofillSaveDraftExtraction.Rejected(AutofillSaveDraftRejection.PASSWORD_MISSING),
            result,
        )
    }

    @Test
    fun `non-password text cannot become a saved password`() {
        val extractor = AutofillSaveDraftExtractor(strategy("user" to FieldRole.USERNAME))

        val result = extractor(request(username = "looks-secret", password = null))

        assertEquals(
            AutofillSaveDraftExtraction.Rejected(AutofillSaveDraftRejection.PASSWORD_MISSING),
            result,
        )
    }

    @Test
    fun `focused password wins when the page contains multiple password candidates`() {
        val extractor = AutofillSaveDraftExtractor(
            strategy(
                "old-pass" to FieldRole.PASSWORD,
                "active-pass" to FieldRole.PASSWORD,
            ),
        )
        val request = request(
            fields = listOf(
                field("old-pass", "old-secret"),
                field("active-pass", "new-secret", isFocused = true),
            ),
        )

        val result = extractor(request)

        assertEquals("new-secret", (result as AutofillSaveDraftExtraction.Ready).draft.password)
    }

    @Test
    fun `multiple unfocused password candidates are rejected instead of depending on tree order`() {
        val extractor = AutofillSaveDraftExtractor(
            strategy(
                "first-pass" to FieldRole.PASSWORD,
                "second-pass" to FieldRole.PASSWORD,
            ),
        )
        val request = request(
            fields = listOf(
                field("first-pass", "first-secret"),
                field("second-pass", "second-secret"),
            ),
        )

        val result = extractor(request)

        assertEquals(
            AutofillSaveDraftExtraction.Rejected(AutofillSaveDraftRejection.PASSWORD_AMBIGUOUS),
            result,
        )
    }

    @Test
    fun `multiple unfocused username candidates are omitted instead of being paired by tree order`() {
        val extractor = AutofillSaveDraftExtractor(
            strategy(
                "first-user" to FieldRole.USERNAME,
                "second-user" to FieldRole.USERNAME,
                "pass" to FieldRole.PASSWORD,
            ),
        )
        val request = request(
            fields = listOf(
                field("first-user", "alice"),
                field("second-user", "bob"),
                field("pass", "secret"),
            ),
        )

        val result = extractor(request)

        assertEquals("", (result as AutofillSaveDraftExtraction.Ready).draft.username)
    }

    private fun request(
        packageName: String = "com.example.app",
        username: String? = "alice",
        password: String? = "secret",
        fields: List<AutofillField>? = null,
    ) = AutofillRequest(
        packageName = packageName,
        domain = "example.com",
        fields = fields ?: buildList {
            username?.let { add(field("user", it)) }
            password?.let { add(field("pass", it)) }
        },
        source = AutofillSource.AUTOFILL_SERVICE,
    )

    private fun field(id: String, text: String, isFocused: Boolean = false) = AutofillField(
        id = id,
        hints = emptySet(),
        inputType = null,
        isFocused = isFocused,
        text = text,
    )

    private fun strategy(vararg roles: Pair<String, FieldRole>) = object : FieldMatchStrategy {
        override fun match(request: AutofillRequest): MatchResult = MatchResult(
            roleMap = roles.toMap(),
            hasCredentials = roles.any { it.second == FieldRole.PASSWORD },
        )
    }
}
