package com.aozijx.passly.presentation.feature.settings.main.interaction

import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.ui.settings.interaction.SwipeActionUiModel
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

        assertEquals(SwipeActionUiModel.DELETE, result.swipeLeftAction)
        assertEquals(SwipeActionUiModel.COPY_USERNAME, result.swipeRightAction)
        SwipeActionUiModel.entries.forEach {
            assertEquals(SwipeActionType.valueOf(it.name), it.toFeatureModel())
        }
    }
}
