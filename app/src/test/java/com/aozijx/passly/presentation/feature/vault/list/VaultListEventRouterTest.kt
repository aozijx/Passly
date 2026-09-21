package com.aozijx.passly.presentation.feature.vault.list

import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultAddTypeUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultListEventRouterTest {
    @Test
    fun routesUiEventsToActionsOrNavigationOwners() {
        val actions = mutableListOf<VaultUiAction>()
        var addOtpOpened = false
        val navigation = VaultNavigation(
            onSettingsClick = {},
            onAddPassword = {},
            onAddOtp = { addOtpOpened = true },
            onAddBankCard = {},
            onShowDetail = {},
        )

        dispatchVaultListEvent(
            event = VaultListEvent.SearchQueryChanged("mail"),
            onAction = actions::add,
            navigation = navigation,
        )
        dispatchVaultListEvent(
            event = VaultListEvent.AddTypeSelected(VaultAddTypeUiModel.TOTP),
            onAction = actions::add,
            navigation = navigation,
        )
        dispatchVaultListEvent(
            event = VaultListEvent.AddTypeSelected(VaultAddTypeUiModel.WIFI),
            onAction = actions::add,
            navigation = navigation,
        )

        assertEquals(VaultUiAction.SearchQueryChanged("mail"), actions[0])
        assertEquals(VaultUiAction.AddTypeSelected(AddType.WIFI), actions[1])
        assertTrue(addOtpOpened)
    }
}
