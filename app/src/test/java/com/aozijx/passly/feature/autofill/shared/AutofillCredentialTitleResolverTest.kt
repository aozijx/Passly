package com.aozijx.passly.feature.autofill.shared

import com.aozijx.passly.domain.autofill.port.ApplicationLabelResolver
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.FieldKey
import org.junit.Assert.assertEquals
import org.junit.Test

class AutofillCredentialTitleResolverTest {

    @Test
    fun draftNormalizesLookupAssociationsAndPreservesCredentialValues() {
        val draft = buildAutofillCredentialDraft(
            packageName = " COM.Example.Mail ",
            webDomain = "https://Login.Example.com/path",
            pageTitle = "Example Login",
            usernameValue = " alice ",
            passwordValue = " secret ",
            applicationLabelResolver = ApplicationLabelResolver { "Example Mail" },
        )

        assertEquals(EntryDraftValue.Text("Example Login"), draft[FieldKey.TITLE])
        assertEquals(EntryDraftValue.Text("alice"), draft[FieldKey.USERNAME])
        assertEquals(EntryDraftValue.Text(" secret "), draft[FieldKey.PASSWORD])
        assertEquals(
            EntryDraftValue.TextList(listOf("login.example.com")),
            draft[FieldKey.DOMAINS],
        )
        assertEquals(
            EntryDraftValue.TextList(listOf("com.example.mail")),
            draft[FieldKey.APPLICATION_IDS],
        )
    }

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
