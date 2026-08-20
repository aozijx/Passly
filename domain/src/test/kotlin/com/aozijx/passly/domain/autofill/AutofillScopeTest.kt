package com.aozijx.passly.domain.autofill

import java.net.URISyntaxException
import org.junit.Assert.assertEquals
import org.junit.Test

class AutofillScopeTest {

    @Test
    fun `normalizeApplicationId trims and lowercases package names`() {
        assertEquals(
            "com.example.app",
            AutofillScope.normalizeApplicationId("  COM.EXAMPLE.APP "),
        )
    }

    @Test
    fun `normalizeDomain preserves the exact host scope`() {
        assertEquals(
            "accounts.google.com",
            AutofillScope.normalizeDomain("https://www.accounts.google.com/login"),
        )
    }

    @Test(expected = URISyntaxException::class)
    fun `normalizeDomain propagates malformed domain errors`() {
        AutofillScope.normalizeDomain("not a valid domain")
    }
}
