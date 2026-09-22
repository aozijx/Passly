package com.aozijx.passly.presentation.feature.autofill.credential

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialResponseOwnershipBoundaryTest {
    @Test
    fun `activity owns platform result while route owns compose state`() {
        val root = sourceRoot()
        val activity = root.resolve(
            "com/aozijx/passly/presentation/feature/autofill/credential/" +
                "CredentialResponseActivity.kt",
        ).readText()
        val route = root.resolve(
            "com/aozijx/passly/presentation/feature/autofill/credential/" +
                "CredentialResponseRoute.kt",
        )

        assertTrue(route.isFile)
        assertEquals(1, Regex("\\bsetContent\\s*\\{").findAll(activity).count())
        assertFalse(activity.contains("state.collect"))
        assertFalse(activity.contains("collectLatest"))
        assertTrue(activity.contains("setResult("))
        assertTrue(route.readText().contains("collectAsStateWithLifecycle"))
        assertTrue(route.readText().contains("LaunchedEffect"))
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
