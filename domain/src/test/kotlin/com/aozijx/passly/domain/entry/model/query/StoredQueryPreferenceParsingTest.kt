package com.aozijx.passly.domain.entry.model.query

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoredQueryPreferenceParsingTest {
    @Test
    fun `hierarchy mode distinguishes missing unsupported and supported keys`() {
        assertEquals(
            EntryHierarchyDisplayModeParseResult.Missing,
            EntryHierarchyDisplayMode.parseStoredKey(null),
        )
        assertEquals(
            EntryHierarchyDisplayModeParseResult.Unsupported("stacked"),
            EntryHierarchyDisplayMode.parseStoredKey("stacked"),
        )
        assertEquals(
            EntryHierarchyDisplayModeParseResult.Supported(EntryHierarchyDisplayMode.EXPANDED),
            EntryHierarchyDisplayMode.parseStoredKey("expanded"),
        )
    }

    @Test
    fun `sort field distinguishes missing unsupported and supported keys`() {
        assertEquals(
            EntrySortFieldParseResult.Missing,
            EntrySortField.parseStoredKey(""),
        )
        assertEquals(
            EntrySortFieldParseResult.Unsupported("POPULARITY"),
            EntrySortField.parseStoredKey("POPULARITY"),
        )
        val supported = EntrySortField.parseStoredKey("UPDATED_AT")
        assertTrue(supported is EntrySortFieldParseResult.Supported)
        assertEquals(
            EntrySortField.UPDATED_AT,
            (supported as EntrySortFieldParseResult.Supported).value,
        )
    }
}
