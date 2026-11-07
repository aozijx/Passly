package com.aozijx.passly.feature.autofill.legacy.service.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillWebOriginResolverTest {
    @Test
    fun equivalentUrlClaimsResolveToOneCanonicalHost() {
        val result = AutofillWebOriginResolver.resolve(listOf("HTTPS://Example.com/login", "example.com:443"))

        assertEquals("example.com", result.domain)
        assertFalse(result.ambiguous)
    }

    @Test
    fun distinctHostsInOneStructureAreAmbiguousEvenWithinOneRegistrableDomain() {
        val result = AutofillWebOriginResolver.resolve(listOf("login.example.com", "shop.example.com"))

        assertTrue(result.ambiguous)
    }

    @Test
    fun invalidClaimCannotBeHiddenByAnotherValidHost() {
        val result = AutofillWebOriginResolver.resolve(listOf("example.com", "https://user@example.com"))

        assertTrue(result.ambiguous)
    }

    @Test
    fun truncatedNativeTreeWithoutWebClaimsRemainsPackageScoped() {
        val result = AutofillWebOriginResolver.resolve(emptyList(), structureTruncated = true)

        assertEquals(null, result.domain)
        assertFalse(result.ambiguous)
    }

    @Test
    fun truncatedTreeWithWebClaimIsAmbiguous() {
        val result = AutofillWebOriginResolver.resolve(listOf("example.com"), structureTruncated = true)

        assertEquals("example.com", result.domain)
        assertTrue(result.ambiguous)
    }
}
