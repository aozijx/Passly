package com.aozijx.passly.presentation.feature.vault.detail.ui.component

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpCardBoundaryTest {
    @Test
    fun `detail body owns qr presentation with the core encoder`() {
        val sourceRoot = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val body = File(
            sourceRoot,
            "com/aozijx/passly/presentation/feature/vault/detail/ui/DetailBody.kt",
        ).readText()

        assertFalse(body.contains("com.aozijx.passly.app.qr"))
        assertTrue(body.contains("com.aozijx.passly.core.platform.qr.QrCodeEncoder"))
    }
    @Test
    fun `totp ui does not encode qr data`() {
        val sourceRoot = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        listOf("TotpCard.kt", "TotpSection.kt").forEach { fileName ->
            val source = File(
                sourceRoot,
                "com/aozijx/passly/presentation/feature/vault/detail/ui/component/$fileName",
            ).readText()

            assertFalse("$fileName must not encode QR data", source.contains("QrCodeEncoder"))
            assertFalse("$fileName must not receive a TOTP URI", source.contains("totpUri"))
        }
    }
}
