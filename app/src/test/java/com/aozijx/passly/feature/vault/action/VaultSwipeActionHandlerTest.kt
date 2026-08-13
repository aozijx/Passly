package com.aozijx.passly.feature.vault.action

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.data.settings.model.SwipeActionType
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultSwipeActionHandlerTest {

    private val item = EntryListItem(
        identity = EntryIdentity(
            id = EntryId("entry"),
            type = EntryType.LOGIN,
            timestamps = EntryTimestamps(0L),
        ),
        profile = EntryProfile(title = "Example", username = "user"),
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
