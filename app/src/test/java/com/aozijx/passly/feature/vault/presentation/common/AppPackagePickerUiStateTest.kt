package com.aozijx.passly.feature.vault.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Test

class AppPackagePickerUiStateTest {
    @Test
    fun emptyCatalogProducesNoAppsResult() {
        val state = appPackagePickerReady(emptyList<App>())

        assertEquals(AppPackagePickerResults.NoApps, state.results)
    }

    @Test
    fun whitespaceQueryShowsEveryAppAndPreservesInput() {
        val apps = listOf(App("Mail", "mail.example"), App("Calendar", "calendar.example"))
        val state = appPackagePickerReady(apps).search("  ", App::label, App::packageName)

        assertEquals("  ", state.query)
        assertEquals(AppPackagePickerResults.Matches(apps), state.results)
    }

    @Test
    fun searchMatchesLabelsWithoutCaseSensitivity() {
        val mail = App("Mail", "mail.example")
        val apps = listOf(mail, App("Calendar", "calendar.example"))
        val state = appPackagePickerReady(apps).search("mAiL", App::label, App::packageName)

        assertEquals(AppPackagePickerResults.Matches(listOf(mail)), state.results)
    }

    @Test
    fun searchMatchesPackageNamesWithoutCaseSensitivity() {
        val mail = App("Email", "com.Example.Mail")
        val apps = listOf(mail, App("Calendar", "calendar.example"))
        val state = appPackagePickerReady(apps).search("EXAMPLE.MAIL", App::label, App::packageName)

        assertEquals(AppPackagePickerResults.Matches(listOf(mail)), state.results)
    }

    @Test
    fun unmatchedSearchProducesNoMatches() {
        val apps = listOf(App("Mail", "mail.example"))
        val state = appPackagePickerReady(apps).search("chat", App::label, App::packageName)

        assertEquals(AppPackagePickerResults.NoMatches, state.results)
    }

    @Test
    fun mappingPreservesQueryAndMapsFullCatalogAndVisibleResults() {
        val mail = App("Mail", "mail.example")
        val state =
            appPackagePickerReady(
                listOf(mail, App("Calendar", "calendar.example")),
            ).search("mail", App::label, App::packageName)

        val mapped = state.mapItems { Item(it.label, it.packageName) }

        assertEquals("mail", mapped.query)
        assertEquals(
            listOf(Item("Mail", "mail.example"), Item("Calendar", "calendar.example")),
            mapped.apps,
        )
        assertEquals(AppPackagePickerResults.Matches(listOf(Item("Mail", "mail.example"))), mapped.results)
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
