package com.aozijx.passly.presentation.feature.scanner.ui

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
            "com/aozijx/passly/presentation/feature/scanner/ui/ScannerContent.kt",
        ).readText()
        val legacyRoot = File(
            sourceRoot,
            "com/aozijx/passly/presentation/ui/scanner",
        )

        assertFalse(
            "Scanner UI still has a parallel presentation ui root",
            legacyRoot.walkTopDown().any { it.isFile && it.extension == "kt" },
        )

        listOf(
            "androidx.camera",
            "BarcodeScanning",
            "InputImage",
            "rememberPermissionRequestHost",
            "TelemetryRuntime",
            "Intent(",
            "Toast.",
            "DisposableEffect",
        ).forEach { forbidden ->
            assertFalse("ScannerContent must not reference $forbidden", source.contains(forbidden))
        }
    }
}
