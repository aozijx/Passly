package com.aozijx.passly.presentation.feature.scanner

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerPresentationOwnershipBoundaryTest {

    @Test
    fun `scanner route owns view model and screen stays passive`() {
        val root = sourceRoot()
        val feature = root.resolve("com/aozijx/passly/presentation/feature/scanner")
        val route = feature.resolve("ScannerRoute.kt")
        val screen = feature.resolve("ui/ScannerScreen.kt")

        assertTrue(route.exists())
        assertTrue(screen.exists())
        assertFalse(feature.resolve("VaultScanner.kt").exists())

        val routeSource = route.readText()
        val screenSource = screen.readText()
        val routeSignature = routeSource.substringAfter("fun ScannerRoute(").substringBefore(") {")

        assertTrue(routeSource.contains("hiltViewModel<ScannerViewModel>()"))
        assertTrue(routeSource.contains("ScannerScreen("))
        assertFalse(routeSignature.contains("viewModel:"))
        assertFalse(screenSource.contains("ScannerViewModel"))
        assertFalse(screenSource.contains("mutableStateOf"))
        assertFalse(screenSource.contains("ScannerEffect"))
    }

    @Test
    fun `scanner result has one state owner instead of a success effect bridge`() {
        val root = sourceRoot()
        val feature = root.resolve("com/aozijx/passly/presentation/feature/scanner")
        val state = feature.resolve("ScannerUiState.kt").readText()
        val effect = feature.resolve("ScannerEffect.kt").readText()

        assertTrue(state.contains("val scanResult: String"))
        assertTrue(state.contains("val scannedOtp: OtpConfig?"))
        assertFalse(effect.contains("ScanSuccess"))
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
