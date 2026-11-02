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
    fun `domain normalization distinguishes missing invalid IDN and canonical ASCII`() {
        assertEquals(AutofillDomainNormalization.Missing, AutofillScope.normalizeDomain(null))
        assertEquals(AutofillDomainNormalization.Missing, AutofillScope.normalizeDomain("  "))
        assertEquals(AutofillDomainNormalization.Invalid, AutofillScope.normalizeDomain("not a valid domain"))
        assertEquals(AutofillDomainNormalization.Invalid, AutofillScope.normalizeDomain("faß.de"))
        assertEquals(
            AutofillDomainNormalization.Valid("xn--bcher-kva.de"),
            AutofillScope.normalizeDomain("bücher.de"),
        )
        assertEquals(
            AutofillScope.normalizeDomain("bücher.de"),
            AutofillScope.normalizeDomain("bu\u0308cher.de"),
        )
        assertEquals(
            AutofillScope.normalizeDomain("bücher.de"),
            AutofillScope.normalizeDomain("bücher\u3002de"),
        )
        assertEquals(
            AutofillScope.normalizeDomain("bücher.de"),
            AutofillScope.normalizeDomain("xn--bcher-kva.de"),
        )
        assertEquals(
            AutofillDomainNormalization.Invalid,
            AutofillScope.normalizeDomain("xn--a.de"),
        )
    }

    @Test
    fun `domain normalization canonicalizes bracketed ipv6 identity`() {
        assertEquals(
            AutofillDomainNormalization.Valid("[2001:db8::1]"),
            AutofillScope.normalizeDomain("https://[2001:0db8:0:0:0:0:0:1]:443/login"),
        )
        assertEquals(
            AutofillDomainNormalization.Valid("[2001:db8::1]"),
            AutofillScope.normalizeDomain("[2001:db8::1]"),
        )
        assertEquals(
            AutofillDomainNormalization.Valid("[2001::1:0:0:1:1]"),
            AutofillScope.normalizeDomain("[2001:0:0:1:0:0:1:1]"),
        )
        assertEquals(
            AutofillDomainNormalization.Valid("[::ffff:192.0.2.1]"),
            AutofillScope.normalizeDomain("[::ffff:192.0.2.1]"),
        )
        assertEquals(
            AutofillScope.normalizeDomain("[::ffff:192.0.2.1]"),
            AutofillScope.normalizeDomain("[0:0:0:0:0:ffff:c000:201]"),
        )
        assertEquals(AutofillDomainNormalization.Invalid, AutofillScope.normalizeDomain("https://[fe80::1%25eth0]/"))
        assertEquals(AutofillDomainNormalization.Invalid, AutofillScope.normalizeDomain("[2001:db8:::1]"))
    }
}
