package com.aozijx.passly.presentation.feature.settings.main.interaction

import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.ui.vault.list.model.VaultSwipeActionUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class InteractionSettingsUiMapperTest {

    @Test
    fun `maps swipe actions across feature and ui boundary`() {
        val result = InteractionSettingsUiState(
            isSwipeEnabled = true,
            swipeLeftAction = SwipeActionType.DELETE,
            swipeRightAction = SwipeActionType.COPY_USERNAME,
        ).toUiModel()

        assertEquals(VaultSwipeActionUiModel.DELETE, result.swipeLeftAction)
        assertEquals(VaultSwipeActionUiModel.COPY_USERNAME, result.swipeRightAction)
        VaultSwipeActionUiModel.entries.forEach {
            assertEquals(SwipeActionType.valueOf(it.name), it.toFeatureModel())
        }
    }
}
