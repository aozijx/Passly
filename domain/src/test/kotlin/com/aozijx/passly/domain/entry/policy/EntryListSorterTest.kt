package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.EntryUsage
import com.aozijx.passly.domain.entry.model.query.SortDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryListSorterTest {

    @Test
    fun `last used keeps unused entries last in both directions`() {
        val items = listOf(
            item("unused", lastUsedAt = null),
            item("older", lastUsedAt = 100L),
            item("newer", lastUsedAt = 200L),
        )

        fun sorted(direction: SortDirection) = EntryListSorter.sort(
            items,
            EntrySort(
                field = EntrySortField.LAST_USED_AT,
                direction = direction,
                pinFavorites = false,
            ),
        ).map { it.id.value }

        assertEquals(listOf("older", "newer", "unused"), sorted(SortDirection.ASC))
        assertEquals(listOf("newer", "older", "unused"), sorted(SortDirection.DESC))
    }

    @Test
    fun `entry type uses declaration order`() {
        val items = listOf(
            item("note", type = EntryType.NOTE),
            item("account", type = EntryType.ACCOUNT),
            item("login", type = EntryType.LOGIN),
        )

        val result = EntryListSorter.sort(
            items,
            EntrySort(
                field = EntrySortField.ENTRY_TYPE,
                direction = SortDirection.ASC,
                pinFavorites = false,
            ),
        )

        assertEquals(listOf("account", "login", "note"), result.map { it.id.value })
    }

    private fun item(
        id: String,
        type: EntryType = EntryType.LOGIN,
        lastUsedAt: Long? = null,
    ) = EntryListItem(
        identity = EntryIdentity(
            id = EntryId(id),
            type = type,
            timestamps = EntryTimestamps(createdAtMs = 1L),
        ),
        profile = EntryProfile(title = id),
        usage = EntryUsage(lastUsedAtMs = lastUsedAt),
    )
}
