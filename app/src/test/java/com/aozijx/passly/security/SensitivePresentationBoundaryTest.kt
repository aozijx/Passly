package com.aozijx.passly.security

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitivePresentationBoundaryTest {

    @Test
    fun `saved state never persists recovery draft identifiers or plaintext`() {
        val source = source(
            "com/aozijx/passly/presentation/feature/settings/security/RecoveryDraftViewModel.kt"
        )

        assertFalse(source.contains("recoveryDraftGenerationId"))
        assertFalse(source.contains("savedStateHandle[DRAFT_GENERATION_ID]"))
        assertTrue(source.contains("savedStateHandle[WAS_DISCLOSURE_OPEN] = true"))
    }

    @Test
    fun `sensitive view model cleanup does not launch on cancelled view model scope`() {
        listOf(
            "com/aozijx/passly/presentation/feature/autofill/legacy/AutofillFillViewModel.kt",
            "com/aozijx/passly/presentation/feature/autofill/credential/CredentialResponseViewModel.kt",
        ).forEach { path ->
            val onCleared = source(path).substringAfter("override fun onCleared()")
                .substringBefore("\n    }")
            assertFalse("Cleanup must not launch in viewModelScope: $path", onCleared.contains("viewModelScope.launch"))
            assertTrue("Cleanup owner missing: $path", onCleared.contains("closeOnOwnerCleared"))
        }
    }

    @Test
    fun `recovery mode presentation exposes password reset only`() {
        val source = source(
            "com/aozijx/passly/presentation/feature/recovery/RecoveryModeViewModel.kt"
        )

        listOf("Backup", "RecoveryScanner", "RecoveryImporter", "export(", "import(").forEach {
            forbidden -> assertFalse("Recovery mode references $forbidden", source.contains(forbidden))
        }
        assertTrue(source.contains("setAppPassword"))
    }

    private fun source(relativePath: String): String {
        val sourceRoot = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        return File(sourceRoot, relativePath).readText()
    }
}
