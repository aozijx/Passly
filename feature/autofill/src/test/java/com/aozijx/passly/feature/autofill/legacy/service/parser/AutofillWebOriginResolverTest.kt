package com.aozijx.passly.feature.autofill.legacy.service.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class AutofillWebOriginResolverTest {
    @Test
    fun originClaimsAndTruncationResolveAsExpected() {
        val cases =
            listOf(
                ResolutionCase(
                    name = "equivalent URL claims canonicalize",
                    claims = listOf("HTTPS://Example.com/login", "example.com:443"),
                    expectedDomain = "example.com",
                    expectedAmbiguous = false,
                ),
                ResolutionCase(
                    name = "distinct hosts within one registrable domain conflict",
                    claims = listOf("login.example.com", "shop.example.com"),
                    expectedDomain = "login.example.com",
                    expectedAmbiguous = true,
                ),
                ResolutionCase(
                    name = "invalid claim is not hidden by a valid host",
                    claims = listOf("example.com", "https://user@example.com"),
                    expectedDomain = "example.com",
                    expectedAmbiguous = true,
                ),
                ResolutionCase(
                    name = "truncated native tree without web claims stays package scoped",
                    claims = emptyList(),
                    structureTruncated = true,
                    expectedDomain = null,
                    expectedAmbiguous = false,
                ),
                ResolutionCase(
                    name = "truncated tree with a web claim is ambiguous",
                    claims = listOf("example.com"),
                    structureTruncated = true,
                    expectedDomain = "example.com",
                    expectedAmbiguous = true,
                ),
            )

        val expected = cases.map { it.name to (it.expectedDomain to it.expectedAmbiguous) }
        val actual =
            cases.map { case ->
                val result = AutofillWebOriginResolver.resolve(case.claims, case.structureTruncated)
                case.name to (result.domain to result.ambiguous)
            }

        assertEquals(expected, actual)
    }

    private data class ResolutionCase(
        val name: String,
        val claims: List<String?>,
        val structureTruncated: Boolean = false,
        val expectedDomain: String?,
        val expectedAmbiguous: Boolean,
    )
}
