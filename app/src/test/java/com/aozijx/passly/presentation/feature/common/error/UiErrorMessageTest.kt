package com.aozijx.passly.presentation.feature.common.error

import com.aozijx.passly.core.error.model.DatabaseLocked
import com.aozijx.passly.core.error.model.Unexpected
import org.junit.Assert.assertEquals
import org.junit.Test

class UiErrorMessageTest {
    @Test
    fun knownAppErrorUsesPresentationMessage() {
        assertEquals("数据库已锁定，请先解锁", DatabaseLocked().toUiMessage())
    }

    @Test
    fun unexpectedAppErrorUsesCallerFallback() {
        assertEquals("重试失败", Unexpected().toUiMessage("重试失败"))
    }

    @Test
    fun throwableIsNormalizedBeforePresentationMapping() {
        assertEquals("操作失败", IllegalStateException("internal").toUiMessage("操作失败"))
    }
}
