package com.aozijx.passly.presentation.feature.vault.editor.otp

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpEditorOwnershipBoundaryTest {
    @Test
    fun `otp editor route owns its scanner overlay without navigation callback bridge`() {
        val sourceRoot = sourceRoot()
        val route = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/editor/otp/AddOtpEditorRoute.kt",
        ).readText()
        val graph = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/navigation/VaultGraphRegistration.kt",
        ).readText()
        val scannerRoute = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/scanner/navigation/VaultOtpScannerRoute.kt",
        )

        assertTrue(route.contains("VaultScanner("))
        assertFalse(route.contains("scannerContent:"))
        assertFalse(graph.contains("scannerContent"))
        assertFalse(graph.contains("VaultOtpScannerRoute"))
        assertFalse(scannerRoute.exists())
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
