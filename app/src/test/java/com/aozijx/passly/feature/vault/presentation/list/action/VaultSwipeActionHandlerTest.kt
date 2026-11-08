package com.aozijx.passly.feature.vault.presentation.list.action

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.shared.components.EntryVisualCategory
import com.aozijx.passly.feature.vault.presentation.list.model.VaultPasswordCardSecondaryTextUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultPasswordCardSupportingTextUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultSwipeActionHandlerTest {
    private val item =
        VaultListItemUiModel(
            id = "entry",
            entryType = EntryType.LOGIN,
            title = "Example",
            username = "user",
            category = null,
            favorite = false,
            associatedDomain = null,
            associatedAppPackage = null,
            iconName = null,
            iconCustomPath = null,
            iconImageModel = null,
            visualCategory = EntryVisualCategory.ACCOUNT,
            hasPassword = true,
            hasOtp = false,
            otpKind = null,
            otpPreview = null,
            passwordCardSecondaryText = VaultPasswordCardSecondaryTextUiModel.EntryTypeLabel(EntryType.LOGIN),
            passwordCardSupportingText = VaultPasswordCardSupportingTextUiModel.PROTECTED,
        )

    @Test
    fun copyAndDeleteOnlyDispatchTheirRequestedActions() {
        val copies = mutableListOf<FieldKey>()
        val deleted = mutableListOf<String>()

        handleSwipeAction(
            actionType = SwipeActionType.COPY_PASSWORD,
            item = item,
            onQuickDelete = { deleted += it },
            onCopy = { copies += it },
        )
        handleSwipeAction(
            actionType = SwipeActionType.COPY_USERNAME,
            item = item,
            onQuickDelete = { deleted += it },
            onCopy = { copies += it },
        )
        handleSwipeAction(
            actionType = SwipeActionType.DELETE,
            item = item,
            onQuickDelete = { deleted += it },
            onCopy = { copies += it },
        )

        assertEquals(listOf(FieldKey.PASSWORD, FieldKey.USERNAME), copies)
        assertEquals(listOf("entry"), deleted)
    }

    @Test
    fun disabledDirectionDispatchesNoAction() {
        val events = mutableListOf<String>()

        handleSwipeAction(
            actionType = SwipeActionType.DISABLED,
            item = item,
            onQuickDelete = { events += "delete" },
            onCopy = { events += "copy" },
        )

        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun copyRequestUsesOtpOnlyForPasswordOnOtpEntries() {
        val otpItem = item.copy(hasOtp = true)

        assertEquals(
            VaultCopyRequest.Field("entry", EntryType.LOGIN, FieldKey.PASSWORD),
            resolveCopyRequest(item, FieldKey.PASSWORD),
        )
        assertEquals(
            VaultCopyRequest.Otp(entryId = "entry"),
            resolveCopyRequest(otpItem, FieldKey.PASSWORD),
        )
        assertEquals(
            VaultCopyRequest.Field("entry", EntryType.LOGIN, FieldKey.USERNAME),
            resolveCopyRequest(otpItem, FieldKey.USERNAME),
        )
    }
}
