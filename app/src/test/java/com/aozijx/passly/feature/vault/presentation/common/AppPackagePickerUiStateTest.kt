package com.aozijx.passly.feature.vault.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Test

class AppPackagePickerUiStateTest {
    @Test
    fun emptyCatalogProducesNoAppsResult() {
        val state = AppPackagePickerUiState.fromInstalledApps(emptyList<App>())

        assertEquals(AppPackagePickerUiState.NoApps(), state)
    }

    @Test
    fun whitespaceQueryShowsEveryAppAndPreservesInput() {
        val apps = listOf(App("Mail", "mail.example"), App("Calendar", "calendar.example"))
        val state = AppPackagePickerUiState.fromInstalledApps(apps).search("  ", App::label, App::packageName)

        assertEquals("  ", state.query)
        assertEquals(AppPackagePickerUiState.Matches(apps, "  ", apps), state)
    }

    @Test
    fun searchMatchesLabelsWithoutCaseSensitivity() {
        val mail = App("Mail", "mail.example")
        val apps = listOf(mail, App("Calendar", "calendar.example"))
        val state = AppPackagePickerUiState.fromInstalledApps(apps).search("mAiL", App::label, App::packageName)

        assertEquals(listOf(mail), (state as AppPackagePickerUiState.Matches).matchingApps)
    }

    @Test
    fun searchMatchesPackageNamesWithoutCaseSensitivity() {
        val mail = App("Email", "com.Example.Mail")
        val apps = listOf(mail, App("Calendar", "calendar.example"))
        val state = AppPackagePickerUiState.fromInstalledApps(apps).search("EXAMPLE.MAIL", App::label, App::packageName)

        assertEquals(listOf(mail), (state as AppPackagePickerUiState.Matches).matchingApps)
    }

    @Test
    fun unmatchedSearchProducesNoMatches() {
        val apps = listOf(App("Mail", "mail.example"))
        val state = AppPackagePickerUiState.fromInstalledApps(apps).search("chat", App::label, App::packageName)

        assertEquals(AppPackagePickerUiState.NoMatches(apps, "chat"), state)
    }

    @Test
    fun mappingPreservesQueryAndMapsFullCatalogAndVisibleResults() {
        val mail = App("Mail", "mail.example")
        val state =
            AppPackagePickerUiState.fromInstalledApps(
                listOf(mail, App("Calendar", "calendar.example")),
            ).search("mail", App::label, App::packageName)

        val mapped = state.mapItems { Item(it.label, it.packageName) } as AppPackagePickerUiState.Matches

        assertEquals("mail", mapped.query)
        assertEquals(
            listOf(Item("Mail", "mail.example"), Item("Calendar", "calendar.example")),
            mapped.installedApps,
        )
        assertEquals(listOf(Item("Mail", "mail.example")), mapped.matchingApps)
    }

    private data class App(
        val label: String,
        val packageName: String,
    )

    private data class Item(
        val label: String,
        val packageName: String,
    )
}
