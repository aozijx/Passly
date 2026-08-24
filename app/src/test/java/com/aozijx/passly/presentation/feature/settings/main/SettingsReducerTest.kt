package com.aozijx.passly.presentation.feature.settings.main

import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsReducerTest {

    @Test
    fun `loaded settings finish loading and replace both swipe actions`() {
        val result = SettingsReducer.reduce(
            SettingsUiState(isLoading = true),
            SettingsMutation.SettingsLoaded(
                swipeLeftAction = SwipeActionType.DELETE,
                swipeRightAction = SwipeActionType.COPY_USERNAME,
            ),
        )

        assertFalse(result.isLoading)
        assertEquals(SwipeActionType.DELETE, result.swipeLeftAction)
        assertEquals(SwipeActionType.COPY_USERNAME, result.swipeRightAction)
    }

    @Test
    fun `app password availability does not overwrite pending database state`() {
        val result = SettingsReducer.reduce(
            SettingsUiState(isClearingDatabase = true),
            SettingsMutation.AppPasswordAvailabilityChanged(enabled = true),
        )

        assertTrue(result.isAppPasswordEnabled)
        assertTrue(result.isClearingDatabase)
    }

    @Test
    fun `database completion preserves settings`() {
        val initial = SettingsUiState(
            isClearingDatabase = true,
            swipeLeftAction = SwipeActionType.COPY_USERNAME,
        )

        val result = SettingsReducer.reduce(initial, SettingsMutation.DatabaseClearFinished)

        assertFalse(result.isClearingDatabase)
        assertEquals(SwipeActionType.COPY_USERNAME, result.swipeLeftAction)
    }
}
