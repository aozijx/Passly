package com.aozijx.passly.presentation.feature.vault.detail

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailRevisionOwnershipBoundaryTest {
    @Test
    fun `route stays passive and screen owns one revision sheet`() {
        val route = source("DetailRoute.kt")
        val screen = source("ui/DetailScreen.kt")
        val body = source("ui/DetailBody.kt")

        assertFalse(route.contains("EntryRevisionRepository"))
        assertFalse(route.contains("RevisionHistorySheet"))
        assertTrue(screen.contains("RevisionHistorySheet"))
        assertTrue(screen.contains("onAction = onAction"))
        assertTrue(body.contains("ActivityTimelineSection"))
        assertTrue(body.contains("RevisionHistorySection"))
    }

    private fun source(relativePath: String): String = File(
        "src/main/java/com/aozijx/passly/presentation/feature/vault/detail/$relativePath",
    ).readText()
}
