package com.aozijx.passly.domain.autofill.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AutofillTitlePolicyTest {

    @Test
    fun appCredentialUsesExactApplicationLabelInsteadOfWindowIdentifier() {
        val title = AutofillTitlePolicy.resolveSavedCredentialTitle(
            pageTitle = "1722183399000",
            domain = null,
            appLabel = "WhatsApp",
            packageName = "com.whatsapp",
            fallback = "Login"
        )

        assertEquals("WhatsApp", title)
    }

    @Test
    fun appCredentialFallsBackToReadablePackageSegment() {
        val title = AutofillTitlePolicy.resolveSavedCredentialTitle(
            pageTitle = "889977665544",
            domain = null,
            appLabel = null,
            packageName = "com.example.mail",
            fallback = "Login"
        )

        assertEquals("Mail", title)
    }

    @Test
    fun webCredentialRejectsOpaqueNumericWindowTitle() {
        val opaqueTitle = "1722183399000"
        val title = AutofillTitlePolicy.resolveSavedCredentialTitle(
            pageTitle = opaqueTitle,
            domain = "accounts.example.com",
            appLabel = "Browser",
            packageName = "com.example.browser",
            fallback = "Login"
        )

        assertNotEquals(opaqueTitle, title)
        assertEquals("accounts.example", title)
    }

    @Test
    fun webCredentialKeepsReadablePageTitle() {
        val title = AutofillTitlePolicy.resolveSavedCredentialTitle(
            pageTitle = "Example Account",
            domain = "accounts.example.com",
            appLabel = "Browser",
            packageName = "com.example.browser",
            fallback = "Login"
        )

        assertEquals("Example Account", title)
    }
}
