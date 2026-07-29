package com.aozijx.passly.feature.vault.action

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.settings.model.SwipeActionType
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultSwipeActionHandlerTest {

    private val item = EntryListItem(
        id = "entry",
        title = "Example",
        username = "user",
        entryType = EntryType.LOGIN,
        icon = null,
        iconCustomPath = null,
        website = null,
        favorite = false,
        tags = emptyList(),
        color = null,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
        expiresAt = null,
        lastUsedAt = null,
        usageCount = 0,
        entryVersion = 1,
        capabilityFlags = 0
    )

    @Test
    fun copyUsesCopyAuthorizationAndDeleteUsesDeleteAuthorization() {
        val events = mutableListOf<String>()

        handleSwipeAction(
            actionType = SwipeActionType.COPY_PASSWORD,
            item = item,
            onDeleteAuthRequired = { events += "delete-auth"; it() },
            onCopyAuthRequired = { events += "copy-auth"; it() },
            onQuickDelete = { events += "delete" },
            onShowDetail = { events += "detail" },
            onCopy = { events += "copy" }
        )
        handleSwipeAction(
            actionType = SwipeActionType.DELETE,
            item = item,
            onDeleteAuthRequired = { events += "delete-auth"; it() },
            onCopyAuthRequired = { events += "copy-auth"; it() },
            onQuickDelete = { events += "delete" },
            onShowDetail = { events += "detail" },
            onCopy = { events += "copy" }
        )

        assertEquals(
            listOf("copy-auth", "copy", "delete-auth", "delete"),
            events
        )
    }
}
