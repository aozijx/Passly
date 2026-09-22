package com.aozijx.passly.presentation.feature.autofill.legacy

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillFillOwnershipBoundaryTest {
    @Test
    fun `activity owns platform result while route owns compose state`() {
        val root = sourceRoot()
        val activity = root.resolve(
            "com/aozijx/passly/presentation/feature/autofill/legacy/AutofillFillActivity.kt",
        ).readText()
        val route = root.resolve(
            "com/aozijx/passly/presentation/feature/autofill/legacy/AutofillFillRoute.kt",
        )
        val ui = root.resolve(
            "com/aozijx/passly/presentation/feature/autofill/legacy/ui/" +
                "AutofillCandidateBottomSheet.kt",
        )

        assertTrue(route.isFile)
        assertTrue(ui.isFile)
        assertEquals(1, Regex("\\bsetContent\\s*\\{").findAll(activity).count())
        assertFalse(activity.contains("uiState.collect"))
        assertFalse(activity.contains("showBottomSheet"))
        assertFalse(activity.contains("toUiItem"))
        assertTrue(route.readText().contains("collectAsStateWithLifecycle"))
        assertFalse(ui.readText().contains("ViewModel"))

        val legacyUi = root.resolve("com/aozijx/passly/presentation/ui/autofill")
        assertFalse(legacyUi.walkTopDown().any { it.isFile && it.extension == "kt" })
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
