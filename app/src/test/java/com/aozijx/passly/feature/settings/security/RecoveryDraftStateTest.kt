package com.aozijx.passly.feature.settings.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecoveryDraftStateTest {
    @Test
    fun expiredDraftHasExplicitNonSecretRecoveryMessage() {
        assertEquals(
            "恢复码草稿已过期，请重新认证后生成。",
            RecoveryDraftState.DraftExpired.messageOrNull()
        )
        assertNull(RecoveryDraftState.Empty.messageOrNull())
    }
}
