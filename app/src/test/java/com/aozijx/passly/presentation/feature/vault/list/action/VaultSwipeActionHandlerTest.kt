package com.aozijx.passly.presentation.feature.vault.list.action

import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.ui.shared.entry.EntryTypeUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultSwipeActionHandlerTest {

    private val item = VaultListItemUiModel(
        id = "entry",
        entryType = EntryTypeUiModel.LOGIN,
        title = "Example",
        username = "user",
        category = null,
        favorite = false,
        associatedDomain = null,
        associatedAppPackage = null,
        iconName = null,
        iconCustomPath = null,
        hasPassword = true,
        hasOtp = false,
        otpKind = null,
        otpPreview = null,
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
