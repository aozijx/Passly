package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.domain.settings.model.SwipeActionType
import org.junit.Assert.assertEquals
import org.junit.Test

class InteractionProtoMappingsTest {
    @Test
    fun `disabled swipe direction has a stable storage key`() {
        assertEquals("disabled", SwipeActionType.DISABLED.toSwipeActionString())
        assertEquals(SwipeActionType.DISABLED, "disabled".toSwipeActionDomain())
    }
}
