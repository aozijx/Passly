package com.aozijx.passly.presentation.feature.vault.detail

import org.junit.Assert.assertEquals
import org.junit.Test

class DetailInstalledAppPresentationTest {
    @Test
    fun `presentation mapping converts feature app metadata at the ui boundary`() {
        val app = DetailInstalledApp(
            label = "Browser",
            packageName = "com.example.browser",
        )

        val actual = app.toPackagePickerItemUiModel()

        assertEquals("Browser", actual.label)
        assertEquals("com.example.browser", actual.packageName)
    }
}
