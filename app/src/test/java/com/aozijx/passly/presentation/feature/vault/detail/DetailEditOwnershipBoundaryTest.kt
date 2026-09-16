package com.aozijx.passly.presentation.feature.vault.detail

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailEditOwnershipBoundaryTest {
    @Test
    fun `detail view model state owns notes and association drafts`() {
        val featureRoot = sourceRoot().resolve(
            "com/aozijx/passly/presentation/feature/vault/detail",
        )
        val route = featureRoot.resolve("DetailRoute.kt").readText()
        val body = featureRoot.resolve("binding/DetailBodyBinding.kt").readText()
        val actions = featureRoot.resolve("DetailUiAction.kt").readText()

        assertFalse(featureRoot.resolve("DetailLocalEditState.kt").exists())
        assertFalse(route.contains("DetailLocalEditState"))
        assertFalse(body.contains("localEditState"))
        listOf(
            "StartNotesEdit", "UpdateNotesDraft", "SaveNotes",
            "StartDomainEdit", "UpdateDomainDraft", "SaveDomain",
        ).forEach { action -> assertTrue("Missing $action", actions.contains(action)) }
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}