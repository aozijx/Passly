package com.aozijx.passly.presentation.mapping.gesture

import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.shared.gesture.SwipeActionUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeActionPresentationMapperTest {
    @Test
    fun mapsEverySwipeActionInBothDirections() {
        val pairs = listOf(
            SwipeActionType.DELETE to SwipeActionUiModel.DELETE,
            SwipeActionType.DETAIL to SwipeActionUiModel.DETAIL,
            SwipeActionType.COPY_PASSWORD to SwipeActionUiModel.COPY_PASSWORD,
            SwipeActionType.COPY_USERNAME to SwipeActionUiModel.COPY_USERNAME,
        )

        pairs.forEach { (domain, ui) ->
            assertEquals(ui, domain.toUiModel())
            assertEquals(domain, ui.toDomainModel())
        }
    }
}
