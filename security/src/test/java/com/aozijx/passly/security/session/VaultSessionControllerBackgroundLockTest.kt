package com.aozijx.passly.security.session

import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.settings.port.IdleTimeoutSettings
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.security.autofill.inbox.AutofillInboxKeyProvisioning
import com.aozijx.passly.security.dek.DekManager
import com.aozijx.passly.security.dek.FieldKeyManager
import com.aozijx.passly.security.dek.SensitiveDataKeyManager
import com.aozijx.passly.security.lock.LockStateManager
import java.security.SecureRandom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultSessionControllerBackgroundLockTest {
    @Test
    fun `disabling background lock preserves the existing idle timeout`() =
        runMainTest { dispatcher ->
            val databaseSession = VaultSessionTestDatabaseSession()
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = databaseSession,
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(false),
                )

            runCurrent()
            controller.transition(AuthenticationState.Authenticated(0L))
            controller.onAppForeground()
            advanceTimeBy(500L)
            controller.onAppBackground()
            advanceTimeBy(500L)
            runCurrent()

            assertTrue(databaseSession.softLockCompleted)
            assertEquals(0, databaseSession.sealCount)
        }

    @Test
    fun `background lock waits for configured timeout and is cancelled on foreground return`() =
        runMainTest { dispatcher ->
            val databaseSession = VaultSessionTestDatabaseSession()
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = databaseSession,
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(true),
                )

            runCurrent()
            controller.transition(AuthenticationState.Authenticated(0L))
            controller.onAppBackground()
            advanceTimeBy(999L)
            runCurrent()

            assertEquals(0, databaseSession.sealCount)

            controller.onAppForeground()
            advanceTimeBy(1L)
            runCurrent()

            assertEquals(0, databaseSession.sealCount)
            advanceUntilIdle()
        }

    @Test
    fun `foreground return cannot cancel a background seal after timeout elapsed`() =
        runMainTest { dispatcher ->
            val logger = VaultSessionTestLogger()
            val databaseSession = VaultSessionTestDatabaseSession(pauseSeal = true)
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = databaseSession,
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(true),
                    logger = logger,
                )

            runCurrent()
            controller.transition(AuthenticationState.Authenticated(0L))
            controller.onAppBackground()
            advanceTimeBy(1_000L)
            runCurrent()
            databaseSession.sealStarted.await()

            controller.onAppForeground()
            databaseSession.allowSealToFinish.complete(Unit)
            runCurrent()

            assertEquals(1, databaseSession.sealCount)
            assertEquals(0, logger.errorEvents.count { it == Events.Security.LockFailed })
            assertEquals(1, logger.infoEvents.count { it == Events.Security.LockCompleted })
            advanceUntilIdle()
        }

    @Test
    fun `idle timeout does not cancel its own lock operation or report a false failure`() =
        runMainTest { dispatcher ->
            val logger = VaultSessionTestLogger()
            val bootstrap = VaultSessionTestBootstrapStore()
            val dekManager = DekManager(bootstrap, FieldKeyManager(), logger)
            val sensitiveKeyManager = SensitiveDataKeyManager(bootstrap, dekManager, CoroutineScope(dispatcher), SecureRandom())
            val databaseSession = VaultSessionTestDatabaseSession()
            val lockState = LockStateManager().apply { mark(SecureSessionState.UNLOCKED) }
            val timeout = MutableStateFlow(1L)
            val controller =
                VaultSessionController(
                    dekManager = dekManager,
                    sensitiveDataKeyClearer = sensitiveKeyManager,
                    authorizationPermitRevoker = { },
                    sessionManager = databaseSession,
                    vaultBootstrapStore = bootstrap,
                    lockStateManager = lockState,
                    securityEvents = SecuritySessionEventLogger(logger),
                    idleTimeoutSettings =
                        object : IdleTimeoutSettings {
                            override val isLockOnBackground = MutableStateFlow(false)
                            override val lockTimeout = timeout
                        },
                    autofillInboxKeyProvisioning = AutofillInboxKeyProvisioning { },
                )

            runCurrent()
            controller.transition(AuthenticationState.Authenticated(0L))
            controller.onAppForeground()
            controller.onUserInteraction()
            advanceTimeBy(1L)
            runCurrent()

            assertTrue(databaseSession.softLockCompleted)
            assertEquals(SecureSessionState.SOFT_LOCKED, lockState.state)
            assertEquals(0, logger.errorEvents.count { it == Events.Security.LockFailed })
            assertEquals(1, logger.infoEvents.count { it == Events.Security.LockCompleted })
        }
}
