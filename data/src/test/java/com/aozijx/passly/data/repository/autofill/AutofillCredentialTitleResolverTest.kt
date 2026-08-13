package com.aozijx.passly.data.repository.autofill

import org.junit.Assert.assertEquals
import org.junit.Test

class AutofillCredentialTitleResolverTest {

    @Test
    fun nativeAppUsesApplicationLabelInsteadOfWindowPackageName() {
        val title = resolveAutofillCredentialTitle(
            applicationId = "com.example.mail",
            appLabel = "Example Mail",
            domain = null,
            pageTitle = "com.example.mail",
            usernameValue = "user@example.com",
        )

        assertEquals("Example Mail", title)
    }

    @Test
    fun webFormKeepsWebsiteTitleInsteadOfBrowserApplicationLabel() {
        val title = resolveAutofillCredentialTitle(
            applicationId = "com.android.chrome",
            appLabel = "Chrome",
            domain = "example.com",
            pageTitle = "Example Account",
            usernameValue = "user@example.com",
        )

        assertEquals("Example Account", title)
    }

    @Test
    fun packageLikeWebTitleFallsBackToDomain() {
        val title = resolveAutofillCredentialTitle(
            applicationId = "com.android.chrome",
            appLabel = "Chrome",
            domain = "example.com",
            pageTitle = "com.android.chrome/MainActivity",
            usernameValue = "user@example.com",
        )

        assertEquals("example.com", title)
    }

    @Test
    fun unavailableApplicationLabelNeverExposesPackageNameAsTitle() {
        val title = resolveAutofillCredentialTitle(
            applicationId = "com.example.hidden",
            appLabel = "com.example.hidden",
            domain = null,
            pageTitle = "com.example.hidden",
            usernameValue = "account",
        )

        assertEquals("account", title)
    }
}
