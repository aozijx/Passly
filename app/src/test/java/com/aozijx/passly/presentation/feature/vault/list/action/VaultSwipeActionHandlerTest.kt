package com.aozijx.passly.presentation.feature.vault.list.action

import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.shared.entry.EntryTypeUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListItemUiModel
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
    fun copyAndDeleteOnlyDispatchTheirRequestedActions() {
        val events = mutableListOf<String>()

        handleSwipeAction(
            actionType = SwipeActionType.COPY_PASSWORD,
            item = item,
            onQuickDelete = { events += "delete" },
            onShowDetail = { events += "detail" },
            onCopy = { events += "copy" }
        )
        handleSwipeAction(
            actionType = SwipeActionType.DELETE,
            item = item,
            onQuickDelete = { events += "delete" },
            onShowDetail = { events += "detail" },
            onCopy = { events += "copy" }
        )

        assertEquals(
            listOf("copy", "delete"),
            events
        )
    }

    @Test
    fun fieldCopyCarriesTheDomainEntryType() {
        assertEquals(
            VaultCopyRequest.Field(
                entryId = "entry",
                entryType = com.aozijx.passly.domain.entry.model.EntryType.LOGIN,
                fieldKey = com.aozijx.passly.domain.entry.model.FieldKey.USERNAME,
            ),
            resolveCopyRequest(item, com.aozijx.passly.domain.entry.model.FieldKey.USERNAME),
        )
    }
    @Test
    fun passwordCopyTargetsOtpWhenTheEntryHasOtp() {
        val otpItem = item.copy(hasOtp = true)

        assertEquals(
            VaultCopyRequest.Otp(entryId = "entry"),
            resolveCopyRequest(otpItem, com.aozijx.passly.domain.entry.model.FieldKey.PASSWORD),
        )
    }
}
