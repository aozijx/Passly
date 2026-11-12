package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import com.aozijx.passly.domain.autofill.port.ApplicationLabelResolver
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.FieldKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillCredentialTitleResolverTest {
    @Test
    fun suspendingApplicationLabelIsResolvedBeforeBuildingNativeCredentialDraft() =
        runBlocking {
            var labelResolved = false
            val draft =
                requireNotNull(
                    buildAutofillCredentialDraft(
                        draft =
                            AutofillSaveDraft(
                                packageName = "com.example.mail",
                                webDomain = null,
                                pageTitle = null,
                                username = "alice",
                                password = "secret",
                            ),
                        applicationLabelResolver =
                            ApplicationLabelResolver {
                                delay(1)
                                labelResolved = true
                                "Example Mail"
                            },
                    ),
                )

            assertTrue(labelResolved)
            assertEquals(EntryDraftValue.Text("Example Mail"), draft[FieldKey.TITLE])
        }

    @Test
    fun draftNormalizesLookupAssociationsAndPreservesCredentialValues() =
        runBlocking {
            val draft =
                requireNotNull(
                    buildAutofillCredentialDraft(
                        draft =
                            AutofillSaveDraft(
                                packageName = " COM.Example.Mail ",
                                webDomain = "https://Login.Example.com/path",
                                pageTitle = "Example Login",
                                username = " alice ",
                                password = " secret ",
                            ),
                        applicationLabelResolver = ApplicationLabelResolver { "Example Mail" },
                    ),
                )

            assertEquals(EntryDraftValue.Text("Example Login"), draft[FieldKey.TITLE])
            assertEquals(EntryDraftValue.Text("alice"), draft[FieldKey.USERNAME])
            assertEquals(EntryDraftValue.Text(" secret "), draft[FieldKey.PASSWORD])
            assertEquals(
                EntryDraftValue.TextList(listOf("login.example.com")),
                draft[FieldKey.DOMAINS],
            )
            assertNull(draft[FieldKey.APPLICATION_IDS])
        }

    @Test
    fun nativeAppUsesApplicationLabelInsteadOfWindowPackageName() {
        val title =
            resolveAutofillCredentialTitle(
                applicationId = "com.example.mail",
                appLabel = "Example Mail",
                domain = null,
                pageTitle = "com.example.mail",
                usernameValue = "user@example.com",
            )

        assertEquals("Example Mail", title)
    }

    @Test
    fun nativeCredentialKeepsOnlyApplicationAssociation() =
        runBlocking {
            val draft =
                requireNotNull(
                    buildAutofillCredentialDraft(
                        draft =
                            AutofillSaveDraft(
                                packageName = " COM.EXAMPLE.MAIL ",
                                webDomain = null,
                                pageTitle = "Mail",
                                username = "alice",
                                password = "secret",
                            ),
                        applicationLabelResolver = ApplicationLabelResolver { "Example Mail" },
                    ),
                )

            assertEquals(EntryDraftValue.TextList(listOf("com.example.mail")), draft[FieldKey.APPLICATION_IDS])
            assertNull(draft[FieldKey.DOMAINS])
        }

    @Test
    fun webFormKeepsWebsiteTitleInsteadOfBrowserApplicationLabel() {
        val title =
            resolveAutofillCredentialTitle(
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
        val title =
            resolveAutofillCredentialTitle(
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
        val title =
            resolveAutofillCredentialTitle(
                applicationId = "com.example.hidden",
                appLabel = "com.example.hidden",
                domain = null,
                pageTitle = "com.example.hidden",
                usernameValue = "account",
            )

        assertEquals("account", title)
    }
}
