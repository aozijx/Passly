package com.aozijx.passly.domain.autofill

import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AutofillScopeTest {
    @Test
    fun `application identity trims lowercases and maps blank to missing`() {
        assertEquals("com.example.app", AutofillScope.normalizeApplicationId("  COM.EXAMPLE.APP "))
        assertEquals(null, AutofillScope.normalizeApplicationId("  "))
        assertEquals(null, AutofillGrantContext("  ").normalized())
    }

    @Test
    fun `domain identity ignores URL components but preserves www host`() {
        assertEquals(
            AutofillDomainNormalization.Valid("www.accounts.google.com"),
            AutofillScope.normalizeDomain(" HTTPS://WWW.Accounts.Google.com:8443/login?from=passly "),
        )
        assertEquals(
            AutofillScope.normalizeDomain("example.com"),
            AutofillScope.normalizeDomain("https://example.com:9443/login"),
        )
        assertNotEquals(
            AutofillScope.normalizeDomain("example.com"),
            AutofillScope.normalizeDomain("www.example.com"),
        )
        assertEquals(
            AutofillScope.normalizeDomain("example.com"),
            AutofillScope.normalizeDomain("example.com."),
        )
    }

    @Test
    fun `domain normalization distinguishes missing invalid unicode and punycode`() {
        assertEquals(AutofillDomainNormalization.Missing, AutofillScope.normalizeDomain(null))
        assertEquals(AutofillDomainNormalization.Missing, AutofillScope.normalizeDomain("  "))
        assertEquals(AutofillDomainNormalization.Invalid, AutofillScope.normalizeDomain("not a valid domain"))
        assertEquals(AutofillDomainNormalization.Invalid, AutofillScope.normalizeDomain("faß.de"))
        assertEquals(
            AutofillDomainNormalization.Valid("xn--bcher-kva.de"),
            AutofillScope.normalizeDomain("xn--bcher-kva.de"),
        )
    }

    @Test
    fun `domain normalization accepts bracketed ipv6 without changing identity`() {
        assertEquals(
            AutofillDomainNormalization.Valid("[2001:db8::1]"),
            AutofillScope.normalizeDomain("https://[2001:db8::1]:443/login"),
        )
        assertEquals(
            AutofillScope.normalizeDomain("https://[2001:db8::1]/"),
            AutofillScope.normalizeDomain("[2001:db8::1]"),
        )
        assertNotEquals(
            AutofillScope.normalizeDomain("[2001:db8::1]"),
            AutofillScope.normalizeDomain("[2001:0db8:0:0:0:0:0:1]"),
        )
        assertEquals(AutofillDomainNormalization.Invalid, AutofillScope.normalizeDomain("https://[fe80::1%25eth0]/"))
    }
}
