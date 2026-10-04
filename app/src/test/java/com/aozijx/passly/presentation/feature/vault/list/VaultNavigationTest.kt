package com.aozijx.passly.presentation.feature.vault.list

import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultAddTypeUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultNavigationTest {
    @Test
    fun `built in editor type opens its navigation destination`() {
        var otpOpened = false
        val actions = mutableListOf<VaultUiAction>()

        routeAddTypeSelection(
            type = VaultAddTypeUiModel.TOTP,
            navigation = navigation(onAddOtp = { otpOpened = true }),
            onAction = actions::add,
        )

        assertTrue(otpOpened)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `registry driven type is sent to the page action owner`() {
        val actions = mutableListOf<VaultUiAction>()

        routeAddTypeSelection(
            type = VaultAddTypeUiModel.WIFI,
            navigation = navigation(),
            onAction = actions::add,
        )

        assertEquals(listOf(VaultUiAction.AddTypeSelected(AddType.WIFI)), actions)
    }

    private fun navigation(
        onAddOtp: () -> Unit = {},
    ) = VaultNavigation(
        onSettingsClick = {},
        onAddPassword = {},
        onAddOtp = onAddOtp,
        onAddBankCard = {},
        onShowDetail = {},
    )
}
