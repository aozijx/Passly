package com.aozijx.passly.presentation.feature.settings.main

import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.ui.vault.list.model.VaultSwipeActionUiModel
import com.aozijx.passly.presentation.ui.settings.main.SettingsScreenLocalState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPresentationAdaptersTest {

    @Test
    fun `dialog model maps swipe actions and password validation`() {
        val localState = SettingsScreenLocalState().apply {
            appPasswordCurrent = "current"
            appPasswordNew = "long-enough-password"
            appPasswordConfirm = "long-enough-password"
        }

        val result = buildSettingsDialogsState(
            localState = localState,
            swipeLeftAction = SwipeActionType.DELETE,
            swipeRightAction = SwipeActionType.COPY_USERNAME,
        )

        assertEquals(VaultSwipeActionUiModel.DELETE, result.swipeLeftAction)
        assertEquals(VaultSwipeActionUiModel.COPY_USERNAME, result.swipeRightAction)
        assertTrue(result.isChangePasswordConfirmEnabled)

        localState.appPasswordConfirm = "different"
        assertFalse(
            buildSettingsDialogsState(
                localState,
                SwipeActionType.DELETE,
                SwipeActionType.COPY_USERNAME,
            ).isChangePasswordConfirmEnabled,
        )
    }
}
