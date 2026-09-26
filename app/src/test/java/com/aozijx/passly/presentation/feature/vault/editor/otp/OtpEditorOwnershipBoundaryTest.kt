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

    @Test
    fun `otp editor uses one state and one action sink without ui mapping bridge`() {
        val sourceRoot = sourceRoot()
        val editorRoot = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/editor",
        )
        val featureRoot = editorRoot.resolve("otp")
        val route = featureRoot.resolve("AddOtpEditorRoute.kt").readText()
        val uiRoot = editorRoot.resolve("ui/otp")
        val screen = uiRoot.resolve("AddOtpEditorScreen.kt").readText()
        val configForm = uiRoot.resolve("OtpConfigForm.kt").readText()

        assertFalse(route.contains("toEditorState"))
        assertFalse(route.contains("OtpEditorEventHandler"))
        assertFalse(route.contains("fun updateForm"))
        assertTrue(screen.contains("state: AddOtpCodeState"))
        assertTrue(screen.contains("onAction: (AddOtpAction) -> Unit"))
        assertTrue(configForm.contains("onAction: (AddOtpAction) -> Unit"))
        assertFalse(uiRoot.resolve("OtpEditorContract.kt").exists())
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
