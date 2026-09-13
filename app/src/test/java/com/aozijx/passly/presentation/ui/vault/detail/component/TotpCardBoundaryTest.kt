package com.aozijx.passly.presentation.ui.vault.detail.component

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class TotpCardBoundaryTest {
    @Test
    fun `totp ui does not encode qr data`() {
        val sourceRoot = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        listOf("TotpCard.kt", "TotpSection.kt").forEach { fileName ->
            val source = File(
                sourceRoot,
                "com/aozijx/passly/presentation/ui/vault/detail/component/$fileName",
            ).readText()

            assertFalse("$fileName must not encode QR data", source.contains("QrCodeEncoder"))
            assertFalse("$fileName must not receive a TOTP URI", source.contains("totpUri"))
        }
    }
}
