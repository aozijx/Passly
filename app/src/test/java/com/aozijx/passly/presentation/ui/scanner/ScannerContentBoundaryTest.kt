package com.aozijx.passly.presentation.ui.scanner

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class ScannerContentBoundaryTest {
    @Test
    fun `scanner content is a passive ui component`() {
        val sourceRoot = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val source = File(
            sourceRoot,
            "com/aozijx/passly/presentation/ui/scanner/ScannerContent.kt",
        ).readText()

        listOf(
            "androidx.camera",
            "BarcodeScanning",
            "InputImage",
            "rememberPermissionRequestHost",
            "AppTelemetry",
            "Intent(",
            "Toast.",
            "DisposableEffect",
        ).forEach { forbidden ->
            assertFalse("ScannerContent must not reference $forbidden", source.contains(forbidden))
        }
    }
}
