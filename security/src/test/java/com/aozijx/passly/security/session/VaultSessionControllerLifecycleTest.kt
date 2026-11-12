package com.aozijx.passly.security.session

import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.LockTrigger
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.DatabaseSessionRetryResult
import com.aozijx.passly.domain.settings.port.IdleTimeoutSettings
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.security.authentication.method.credential.OwnedBytes
import com.aozijx.passly.security.autofill.inbox.AutofillInboxKeyProvisioning
import com.aozijx.passly.security.dek.DekManager
import com.aozijx.passly.security.dek.DekState
import com.aozijx.passly.security.dek.FieldKeyManager
import com.aozijx.passly.security.dek.SensitiveDataKeyClearer
import com.aozijx.passly.security.dek.SensitiveDataKeyManager
import com.aozijx.passly.security.lock.LockStateManager
import java.security.SecureRandom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultSessionControllerLifecycleTest {
    @Test
    fun `lock publishes authentication revocation before sensitive key cleanup suspends`() =
        runMainTest { dispatcher ->
            val clearStarted = CompletableDeferred<Unit>()
            val finishClear = CompletableDeferred<Unit>()
            val databaseSession = VaultSessionTestDatabaseSession(pauseSoftLock = true)
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = databaseSession,
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(false),
                    clearSensitiveKey =
                        SensitiveDataKeyClearer {
                            clearStarted.complete(Unit)
                            finishClear.await()
                        },
                )
            controller.transition(AuthenticationState.Authenticated(0L))
            val locking =
                async(dispatcher) {
                    controller.lock(SessionLockRequest(LockReason.USER, LockTrigger.USER_ACTION))
                }

            try {
                databaseSession.softLockStarted.await()

                assertTrue(controller.authenticationState.value is AuthenticationState.Locking)
                assertFalse(controller.hasFullSecureSessionAccess())
                assertFalse("sensitive keys must remain until active leases drain", clearStarted.isCompleted)

                databaseSession.allowSoftLockToFinish.complete(Unit)
                clearStarted.await()

                assertTrue(controller.authenticationState.value is AuthenticationState.Locking)
                assertTrue(databaseSession.softLockCompleted)
                assertFalse(controller.hasFullSecureSessionAccess())

                finishClear.complete(Unit)
                locking.await()
                assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            } finally {
                databaseSession.allowSoftLockToFinish.complete(Unit)
                finishClear.complete(Unit)
                locking.cancel()
            }
        }

    @Test
    fun `fatal reset result is propagated after locking the session`() =
        runMainTest { dispatcher ->
            val fatal = AssertionError("fatal reset result")
            val session = VaultSessionTestDatabaseSession()
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = session,
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(false),
                )
            controller.transition(AuthenticationState.Authenticated(0L))

            val caught =
                try {
                    controller.withExclusiveDatabaseReset { fatal }
                    null
                } catch (error: AssertionError) {
                    error
                }

            assertSame(fatal, caught)
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(SecureSessionState.SEALED, session.lockState)
        }

    @Test
    fun `fatal authorization revocation still clears keys seals and locks`() =
        runMainTest { dispatcher ->
            val fatal = AssertionError("permit revoke fatal")
            val session = VaultSessionTestDatabaseSession()
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = session,
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(false),
                    revokeAuthorization = { throw fatal },
                )
            controller.transition(AuthenticationState.Authenticated(0L))

            val caught =
                try {
                    controller.lock(SessionLockRequest(LockReason.APP_EXIT, LockTrigger.APP_EXIT))
                    null
                } catch (error: AssertionError) {
                    error
                }

            assertSame(fatal, caught)
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(SecureSessionState.SEALED, session.lockState)
        }

    @Test
    fun `fatal sensitive key clear still seals and locks`() =
        runMainTest { dispatcher ->
            val fatal = AssertionError("sensitive key clear fatal")
            val session = VaultSessionTestDatabaseSession()
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = session,
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(false),
                    clearSensitiveKey = SensitiveDataKeyClearer { throw fatal },
                )
            controller.transition(AuthenticationState.Authenticated(0L))

            val caught =
                try {
                    controller.lock(SessionLockRequest(LockReason.APP_EXIT, LockTrigger.APP_EXIT))
                    null
                } catch (error: AssertionError) {
                    error
                }

            assertSame(fatal, caught)
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(SecureSessionState.SEALED, session.lockState)
            assertEquals(1, session.sealCount)
        }

    @Test
    fun `fatal sensitive key clear during reset locks before propagating`() =
        runMainTest { dispatcher ->
            val fatal = AssertionError("sensitive key clear fatal")
            val session = VaultSessionTestDatabaseSession()
            var revoked = 0
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = session,
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(false),
                    revokeAuthorization = { revoked++ },
                    clearSensitiveKey = SensitiveDataKeyClearer { throw fatal },
                )
            controller.transition(AuthenticationState.Authenticated(0L))

            val caught =
                try {
                    controller.withExclusiveDatabaseReset {
                        session.seal()
                        IllegalStateException("ordinary reset failure")
                    }
                    null
                } catch (error: AssertionError) {
                    error
                }

            assertSame(fatal, caught)
            assertEquals(1, revoked)
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(SecureSessionState.SEALED, session.lockState)
        }

    @Test
    fun `fatal close result propagates after lock revokes access and clears keys`() =
        runMainTest { dispatcher ->
            val fatal = AssertionError("database close fatal")
            var revoked = 0
            val session =
                VaultSessionTestDatabaseSession(
                    sealResult = Result.failure(fatal),
                )
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = session,
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(false),
                    revokeAuthorization = { revoked++ },
                )
            controller.transition(AuthenticationState.Authenticated(0L))

            val caught =
                try {
                    controller.lock(SessionLockRequest(LockReason.APP_EXIT, LockTrigger.APP_EXIT))
                    null
                } catch (error: AssertionError) {
                    error
                }

            assertSame(fatal, caught)
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(SecureSessionState.SEALED, session.lockState)
            assertEquals(1, revoked)
        }

    @Test
    fun `fatal authorization revocation still locks and propagates`() =
        runMainTest { dispatcher ->
            val fatal = AssertionError("permit revoke fatal")
            val session = VaultSessionTestDatabaseSession()
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = session,
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(false),
                    revokeAuthorization = { throw fatal },
                )
            controller.transition(AuthenticationState.Authenticated(0L))

            val caught =
                try {
                    controller.withExclusiveDatabaseReset {
                        session.seal()
                        IllegalStateException("ordinary reset failure")
                    }
                    null
                } catch (error: AssertionError) {
                    error
                }

            assertSame(fatal, caught)
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(SecureSessionState.SEALED, session.lockState)
        }

    @Test
    fun `close failure during security lock is reported after fail closed cleanup`() =
        runMainTest { dispatcher ->
            val logger = VaultSessionTestLogger()
            val closeFailure = IllegalStateException("database close failed")
            val session =
                VaultSessionTestDatabaseSession(
                    sealResult = Result.failure(closeFailure),
                )
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = session,
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(false),
                    logger = logger,
                )
            controller.transition(AuthenticationState.Authenticated(0L))

            controller.lock(SessionLockRequest(LockReason.APP_EXIT, LockTrigger.APP_EXIT))

            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(SecureSessionState.SEALED, session.lockState)
            assertEquals(1, logger.errorEvents.count { it == Events.Security.LockFailed })
        }

    @Test
    fun `failed reset close keeps authentication locked and revokes permits`() =
        runMainTest { dispatcher ->
            var revoked = 0
            val closeFailure = IllegalStateException("database close failed")
            val session =
                VaultSessionTestDatabaseSession(
                    sealResult = Result.failure(closeFailure),
                )
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = session,
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(false),
                    revokeAuthorization = { revoked++ },
                )
            controller.transition(AuthenticationState.Authenticated(0L))

            val result =
                controller.withExclusiveDatabaseReset {
                    session.seal().fold(
                        onSuccess = { session.unlock() },
                        onFailure = { it },
                    )
                }

            assertEquals(closeFailure, result)
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(closeFailure, controller.databaseFailure.value)
            assertEquals(SecureSessionState.SEALED, session.lockState)
            assertEquals(1, revoked)
            assertEquals(0, session.unlockCalls)
        }

    @Test
    fun `failed reset reopen publishes locked state and supports retry`() =
        runMainTest { dispatcher ->
            val session = VaultSessionTestDatabaseSession()
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = session,
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(false),
                )
            controller.transition(AuthenticationState.Authenticated(0L))
            val reopenError = IllegalStateException("database reopen failed")

            val result =
                controller.withExclusiveDatabaseReset {
                    session.seal()
                    session.unlockResultError = reopenError
                    session.unlock()
                }

            assertEquals(reopenError, result)
            assertFalse(controller.isUnlocked())
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(reopenError, controller.databaseFailure.value)

            session.unlockResultError = null
            assertEquals(DatabaseSessionRetryResult.Ready, controller.retry())
            assertTrue(controller.hasFullSecureSessionAccess())
            assertEquals(null, controller.databaseFailure.value)
        }

    @Test
    fun `security lock waits for database reset to finish reopening`() =
        runMainTest { dispatcher ->
            val session = VaultSessionTestDatabaseSession()
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = session,
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(false),
                )
            controller.transition(AuthenticationState.Authenticated(0L))
            val resetStarted = CompletableDeferred<Unit>()
            val finishReset = CompletableDeferred<Unit>()
            val reset =
                async(dispatcher) {
                    controller.withExclusiveDatabaseReset {
                        session.seal()
                        resetStarted.complete(Unit)
                        finishReset.await()
                        session.unlock()
                    }
                }
            resetStarted.await()

            val lock =
                async(dispatcher) {
                    controller.lock(SessionLockRequest(LockReason.USER, LockTrigger.USER_ACTION))
                }
            runCurrent()
            assertFalse(lock.isCompleted)

            finishReset.complete(Unit)
            reset.await()
            lock.await()

            assertEquals(SecureSessionState.SOFT_LOCKED, session.lockState)
            assertTrue(controller.isLocked())
        }

    @Test
    fun `database-only seal immediately revokes secure session access`() =
        runMainTest { dispatcher ->
            val databaseSession = VaultSessionTestDatabaseSession()
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = databaseSession,
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(false),
                )
            controller.transition(AuthenticationState.Authenticated(0L))

            databaseSession.lockState = SecureSessionState.SEALED

            assertFalse(controller.isUnlocked())
            assertFalse(controller.hasFullSecureSessionAccess())
            assertTrue(controller.isLocked())
        }

    @Test
    fun `cancelled fresh unlock seals staged session and propagates cancellation`() =
        runMainTest { dispatcher ->
            val bootstrap = VaultSessionTestBootstrapStore()
            val dekManager = DekManager(bootstrap, FieldKeyManager(), VaultSessionTestLogger())
            val session = VaultSessionTestDatabaseSession(pauseUnlock = true)
            val lockState = LockStateManager()
            val controller =
                VaultSessionController(
                    dekManager = dekManager,
                    sensitiveDataKeyClearer = SensitiveDataKeyManager(bootstrap, dekManager, CoroutineScope(dispatcher), SecureRandom()),
                    authorizationPermitRevoker = { },
                    sessionManager = session,
                    vaultBootstrapStore = bootstrap,
                    lockStateManager = lockState,
                    securityEvents = SecuritySessionEventLogger(VaultSessionTestLogger()),
                    idleTimeoutSettings =
                        object : IdleTimeoutSettings {
                            override val isLockOnBackground = MutableStateFlow(false)
                            override val lockTimeout = MutableStateFlow(1_000L)
                        },
                    autofillInboxKeyProvisioning = AutofillInboxKeyProvisioning { },
                )

            val unlockJob =
                launch(dispatcher) {
                    controller.commitUnlock(
                        EnvelopeType.APP_PASSWORD,
                        OwnedBytes(ByteArray(32) { 7 }),
                        "unlock-cancel",
                        AuthenticationPurpose.UNLOCK_VAULT,
                    )
                }
            runCurrent()
            session.unlockStarted.await()
            unlockJob.cancel()
            runCurrent()
            unlockJob.join()

            assertTrue(unlockJob.isCancelled)
            assertTrue(dekManager.state is DekState.Locked)
            assertEquals(SecureSessionState.SEALED, lockState.state)
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(1, session.sealCount)
        }

    @Test
    fun `cancelled soft lock resume preserves existing dek and soft lock`() =
        runMainTest { dispatcher ->
            val cancelled = CancellationException("soft lock resume cancelled")
            val bootstrap = VaultSessionTestBootstrapStore()
            val dekManager = DekManager(bootstrap, FieldKeyManager(), VaultSessionTestLogger())
            dekManager.setDek(EnvelopeType.APP_PASSWORD, ByteArray(32) { 6 })
            val session =
                VaultSessionTestDatabaseSession(
                    initialLockState = SecureSessionState.SOFT_LOCKED,
                    unlockFailure = cancelled,
                )
            val lockState = LockStateManager().apply { mark(SecureSessionState.SOFT_LOCKED) }
            val controller =
                VaultSessionController(
                    dekManager = dekManager,
                    sensitiveDataKeyClearer = SensitiveDataKeyManager(bootstrap, dekManager, CoroutineScope(dispatcher), SecureRandom()),
                    authorizationPermitRevoker = { },
                    sessionManager = session,
                    vaultBootstrapStore = bootstrap,
                    lockStateManager = lockState,
                    securityEvents = SecuritySessionEventLogger(VaultSessionTestLogger()),
                    idleTimeoutSettings =
                        object : IdleTimeoutSettings {
                            override val isLockOnBackground = MutableStateFlow(false)
                            override val lockTimeout = MutableStateFlow(1_000L)
                        },
                    autofillInboxKeyProvisioning = AutofillInboxKeyProvisioning { },
                )

            val thrown =
                try {
                    controller.commitUnlock(
                        EnvelopeType.APP_PASSWORD,
                        OwnedBytes(ByteArray(32) { 9 }),
                        "soft-lock-cancel",
                        AuthenticationPurpose.UNLOCK_VAULT,
                    )
                    null
                } catch (failure: CancellationException) {
                    failure
                }

            assertVaultSessionCancellation(thrown, cancelled)
            assertTrue(dekManager.isUnlocked.value)
            assertEquals(SecureSessionState.SOFT_LOCKED, lockState.state)
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(0, session.sealCount)
        }

    @Test
    fun `cancelled recovery unlock seals staged recovery session and propagates cancellation`() =
        runMainTest { dispatcher ->
            val cancelled = CancellationException("recovery database unlock cancelled")
            val bootstrap = VaultSessionTestBootstrapStore()
            val dekManager = DekManager(bootstrap, FieldKeyManager(), VaultSessionTestLogger())
            val session = VaultSessionTestDatabaseSession(unlockFailure = cancelled)
            val lockState = LockStateManager()
            val controller =
                VaultSessionController(
                    dekManager = dekManager,
                    sensitiveDataKeyClearer = SensitiveDataKeyManager(bootstrap, dekManager, CoroutineScope(dispatcher), SecureRandom()),
                    authorizationPermitRevoker = { },
                    sessionManager = session,
                    vaultBootstrapStore = bootstrap,
                    lockStateManager = lockState,
                    securityEvents = SecuritySessionEventLogger(VaultSessionTestLogger()),
                    idleTimeoutSettings =
                        object : IdleTimeoutSettings {
                            override val isLockOnBackground = MutableStateFlow(false)
                            override val lockTimeout = MutableStateFlow(1_000L)
                        },
                    autofillInboxKeyProvisioning = AutofillInboxKeyProvisioning { },
                )

            val thrown =
                try {
                    controller.commitRecoveryUnlock(OwnedBytes(ByteArray(32) { 8 }), "recovery-cancel")
                    null
                } catch (failure: CancellationException) {
                    failure
                }

            assertVaultSessionCancellation(thrown, cancelled)
            assertTrue(dekManager.state is DekState.Locked)
            assertEquals(SecureSessionState.SEALED, lockState.state)
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(1, session.sealCount)
        }

    @Test
    fun `cancelling a sealed lock completes key cleanup and reports cancellation`() =
        runMainTest { dispatcher ->
            val logger = VaultSessionTestLogger()
            val bootstrap = VaultSessionTestBootstrapStore()
            val dekManager = DekManager(bootstrap, FieldKeyManager(), logger)
            dekManager.setDek(EnvelopeType.APP_PASSWORD, ByteArray(32) { 7 })
            val databaseSession = VaultSessionTestDatabaseSession(pauseSeal = true)
            val lockState = LockStateManager().apply { mark(SecureSessionState.UNLOCKED) }
            val controller =
                VaultSessionController(
                    dekManager = dekManager,
                    sensitiveDataKeyClearer =
                        SensitiveDataKeyManager(
                            bootstrap,
                            dekManager,
                            CoroutineScope(dispatcher),
                            SecureRandom(),
                        ),
                    authorizationPermitRevoker = { },
                    sessionManager = databaseSession,
                    vaultBootstrapStore = bootstrap,
                    lockStateManager = lockState,
                    securityEvents = SecuritySessionEventLogger(logger),
                    idleTimeoutSettings =
                        object : IdleTimeoutSettings {
                            override val isLockOnBackground = MutableStateFlow(false)
                            override val lockTimeout = MutableStateFlow(1_000L)
                        },
                    autofillInboxKeyProvisioning = AutofillInboxKeyProvisioning { },
                )
            runCurrent()
            controller.transition(AuthenticationState.Authenticated(0L))

            val lockJob =
                launch(dispatcher) {
                    controller.lock(SessionLockRequest(LockReason.APP_EXIT, LockTrigger.APP_EXIT))
                }
            runCurrent()
            databaseSession.sealStarted.await()

            lockJob.cancel()
            databaseSession.allowSealToFinish.complete(Unit)
            runCurrent()
            lockJob.join()

            assertTrue("caller cancellation must propagate after cleanup", lockJob.isCancelled)
            assertTrue(dekManager.state is DekState.Locked)
            assertEquals(SecureSessionState.SEALED, lockState.state)
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(1, logger.infoEvents.count { it == Events.Security.LockCancelled })
            assertEquals(0, logger.errorEvents.count { it == Events.Security.LockFailed })
        }

    @Test
    fun `seal cancellation completes lock cleanup then propagates cancellation`() =
        runMainTest { dispatcher ->
            val cancelled = CancellationException("database seal cancelled")
            val logger = VaultSessionTestLogger()
            val bootstrap = VaultSessionTestBootstrapStore()
            val dekManager = DekManager(bootstrap, FieldKeyManager(), logger)
            dekManager.setDek(EnvelopeType.APP_PASSWORD, ByteArray(32) { 7 })
            val databaseSession = VaultSessionTestDatabaseSession(sealFailure = cancelled)
            val lockState = LockStateManager().apply { mark(SecureSessionState.UNLOCKED) }
            val controller =
                VaultSessionController(
                    dekManager = dekManager,
                    sensitiveDataKeyClearer =
                        SensitiveDataKeyManager(
                            bootstrap,
                            dekManager,
                            CoroutineScope(dispatcher),
                            SecureRandom(),
                        ),
                    authorizationPermitRevoker = { },
                    sessionManager = databaseSession,
                    vaultBootstrapStore = bootstrap,
                    lockStateManager = lockState,
                    securityEvents = SecuritySessionEventLogger(logger),
                    idleTimeoutSettings =
                        object : IdleTimeoutSettings {
                            override val isLockOnBackground = MutableStateFlow(false)
                            override val lockTimeout = MutableStateFlow(1_000L)
                        },
                    autofillInboxKeyProvisioning = AutofillInboxKeyProvisioning { },
                )
            controller.transition(AuthenticationState.Authenticated(0L))

            val thrown =
                try {
                    controller.lock(SessionLockRequest(LockReason.APP_EXIT, LockTrigger.APP_EXIT))
                    null
                } catch (failure: CancellationException) {
                    failure
                }

            assertVaultSessionCancellation(thrown, cancelled)
            assertEquals(1, databaseSession.sealCount)
            assertTrue(dekManager.state is DekState.Locked)
            assertEquals(SecureSessionState.SEALED, lockState.state)
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(1, logger.infoEvents.count { it == Events.Security.LockCancelled })
            assertEquals(0, logger.errorEvents.count { it == Events.Security.LockFailed })
        }

    @Test
    fun `soft lock cancellation completes state transition then propagates cancellation`() =
        runMainTest { dispatcher ->
            val cancelled = CancellationException("database soft lock cancelled")
            val logger = VaultSessionTestLogger()
            val databaseSession = VaultSessionTestDatabaseSession(softLockFailure = cancelled)
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = databaseSession,
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(false),
                    logger = logger,
                )
            runCurrent()
            controller.transition(AuthenticationState.Authenticated(0L))

            val thrown =
                try {
                    controller.lock(SessionLockRequest(LockReason.USER, LockTrigger.USER_ACTION))
                    null
                } catch (failure: CancellationException) {
                    failure
                }

            assertVaultSessionCancellation(thrown, cancelled)
            assertTrue(databaseSession.softLockCompleted)
            assertEquals(SecureSessionState.SOFT_LOCKED, databaseSession.lockState)
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
            assertEquals(1, logger.infoEvents.count { it == Events.Security.LockCancelled })
            assertEquals(0, logger.errorEvents.count { it == Events.Security.LockFailed })
        }

    @Test
    fun `fatal soft lock failure escalates to sealed cleanup before propagating`() =
        runMainTest { dispatcher ->
            val fatal = AssertionError("database soft lock fatal")
            val databaseSession = VaultSessionTestDatabaseSession(softLockFailure = fatal)
            val controller =
                createVaultSessionTestController(
                    dispatcher = dispatcher,
                    databaseSession = databaseSession,
                    timeout = MutableStateFlow(1_000L),
                    lockOnBackground = MutableStateFlow(false),
                )
            runCurrent()
            controller.transition(AuthenticationState.Authenticated(0L))

            val caught =
                try {
                    controller.lock(SessionLockRequest(LockReason.USER, LockTrigger.USER_ACTION))
                    null
                } catch (error: AssertionError) {
                    error
                }

            assertSame(fatal, caught)
            assertEquals(1, databaseSession.sealCount)
            assertEquals(SecureSessionState.SEALED, databaseSession.lockState)
            assertEquals(AuthenticationState.Locked, controller.authenticationState.value)
        }
}
