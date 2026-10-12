package com.aozijx.passly.feature.autofill.legacy

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyAutofillFlowTest {
    @Test
    fun viewModelDelegatesToSharedAutofillOperations() {
        val source = source(
            "com/aozijx/passly/presentation/feature/autofill/legacy/AutofillFillViewModel.kt",
        )

        assertTrue(source.contains("AuthorizeAutofillInteraction"))
        assertTrue(source.contains("DiscoverAutofillCandidates"))
        assertTrue(source.contains("ResolveAutofillSelection"))
        assertTrue(source.contains("markFilledAwaitingSave"))
        assertFalse(source.contains("CandidateRetriever"))
        assertFalse(source.contains("AutofillRequestSession"))
        assertFalse(source.contains("InteractionSettingsSource"))
        assertFalse(source.contains("SecureSessionAccessState"))
    }

    @Test
    fun activityResultContractHasNoNullableCompletionPayload() {
        val source = source(
            "com/aozijx/passly/presentation/feature/autofill/legacy/AutofillFillContract.kt",
        )

        assertTrue(source.contains("data object Cancelled : AutofillFillResult"))
        assertFalse(source.contains("AutofillAuthenticationPayload"))
        assertFalse(source.contains("AutofillFillResult?"))
    }

    private fun source(path: String): String = sourceRoot().resolve(path).readText()

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
