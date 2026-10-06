package com.aozijx.passly.presentation.feature.scanner.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerUiOwnershipBoundaryTest {

    @Test
    fun `camera host owns capture only and old scanner content is removed`() {
        val feature = scannerFeatureRoot()
        val cameraHost = feature.resolve("ScannerCameraHost.kt").readText()

        assertFalse(feature.resolve("ui/ScannerContent.kt").exists())
        listOf(
            "android.content.Intent",
            "android.widget.Toast",
            "SensitiveClipboardWriter",
            "ScannerResultCard",
            "ScannerContent",
            "onCopyResult",
            "autoHandleLinks",
        ).forEach { forbidden ->
            assertFalse("Camera host must not reference $forbidden", cameraHost.contains(forbidden))
        }
        assertTrue(cameraHost.contains("BarcodeScanning.getClient()"))
        assertTrue(cameraHost.contains("PreviewView"))
    }

    @Test
    fun `scanner screen is passive and composes semantic scanner ui`() {
        val screen = scannerFeatureRoot().resolve("ui/ScannerScreen.kt").readText()

        listOf(
            "ScannerViewModel",
            "androidx.camera",
            "com.google.mlkit",
            "android.content.Intent",
            "android.widget.Toast",
            "mutableStateOf",
            "SensitiveClipboardWriter",
        ).forEach { forbidden ->
            assertFalse("Scanner screen must not reference $forbidden", screen.contains(forbidden))
        }
        assertTrue(screen.contains("ScannerCameraHost("))
        assertTrue(screen.contains("ScannerViewfinder("))
        assertTrue(screen.contains("ScannerResultCard("))
    }

    private fun scannerFeatureRoot(): File {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        return sourceRoot.resolve("com/aozijx/passly/presentation/feature/scanner")
    }
}
