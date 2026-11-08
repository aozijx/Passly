package com.aozijx.passly.security.session

import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultSessionControllerInboxProvisioningTest {
    @Test
    fun `only a full Vault unlock provisions the Autofill Inbox key`() =
        runMainTest { dispatcher ->
            var provisionCount = 0
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = VaultSessionTestDatabaseSession(),
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(false),
                    provisionInboxKey = { provisionCount++ },
                )

            assertTrue(controller.markAuthenticated(AuthenticationPurpose.UNLOCK_VAULT))
            assertEquals(1, provisionCount)
            assertTrue(controller.markAuthenticated(AuthenticationPurpose.AUTOFILL))
            assertEquals("AUTOFILL must not provision the Inbox key", 1, provisionCount)
        }
}
