package com.aozijx.passly.presentation.feature.vault.list

import androidx.paging.PagingData
import com.aozijx.passly.presentation.ui.shared.entry.EntryTypeUiModel
import com.aozijx.passly.presentation.ui.shared.gesture.SwipeActionUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultQuickFilterUiModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VaultListBindingsTest {
    @Test
    fun `updating callbacks keeps paging flows and dispatches to latest callback`() {
        val passwordPages = flowOf(PagingData.empty<VaultListItemUiModel>())
        val bindings = VaultListBindings(
            entryPages = mapOf(VaultQuickFilterUiModel.PASSWORDS to passwordPages),
        )
        val item = vaultListItem(id = "entry-1")
        val events = mutableListOf<String>()

        bindings.updateEvents(
            onItemClick = { events += "old-click:${it.id}" },
            onItemSwipe = { entry, action -> events += "old-swipe:${entry.id}:$action" },
        )
        val flowBeforeCallbackChange = bindings.entryPages.getValue(VaultQuickFilterUiModel.PASSWORDS)

        bindings.updateEvents(
            onItemClick = { events += "new-click:${it.id}" },
            onItemSwipe = { entry, action -> events += "new-swipe:${entry.id}:$action" },
        )
        bindings.eventHandler.onClick(item)
        bindings.eventHandler.onSwipe(item, SwipeActionUiModel.DELETE)

        assertSame(
            flowBeforeCallbackChange,
            bindings.entryPages.getValue(VaultQuickFilterUiModel.PASSWORDS),
        )
        assertEquals(
            listOf("new-click:entry-1", "new-swipe:entry-1:DELETE"),
            events,
        )
    }

    private fun vaultListItem(id: String) = VaultListItemUiModel(
        id = id,
        entryType = EntryTypeUiModel.LOGIN,
        title = "Mail",
        username = "user@example.com",
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
}
