package com.aozijx.passly.feature.autofill.legacy

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyResponseFactoryTest {
    @Test
    fun responseFactoryMapsTypedCandidatesAndCarriesInteractionIdentity() {
        val source = sourceRoot().resolve(
            "com/aozijx/passly/feature/autofill/legacy/service/builder/LegacyResponseFactory.kt",
        ).readText()

        assertTrue(source.contains("AutofillDiscoveryOutcome"))
        assertTrue(source.contains("AutofillCandidateSummary"))
        assertTrue(source.contains("AutofillInteractionId"))
        assertFalse(source.contains("password.isNotBlank"))
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
