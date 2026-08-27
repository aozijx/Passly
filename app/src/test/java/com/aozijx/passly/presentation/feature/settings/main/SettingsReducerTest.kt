package com.aozijx.passly.presentation.feature.settings.main

import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

}
