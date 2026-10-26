package com.aozijx.passly.feature.vault.presentation.editor

import com.aozijx.passly.core.error.model.Conflict
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftTarget
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorSaveSessionTest {
    @Test
    fun conflatedLockAndUnlockWithTheSameTimestampStillInvalidatesTheOldEffect() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.save()
        runCurrent()
        fixture.access.lock()
        fixture.access.authenticate(1L)
        val effects = mutableListOf<EditorSaveEffect>()
        backgroundScope.launch { fixture.session.effects.toList(effects) }
        runCurrent()
        assertTrue(effects.isEmpty())
        assertTrue(fixture.session.canEdit())
    }

    @Test
    fun duplicateSaveAndEditsAreRejectedWhileTheAttemptIsRunning() = runTest {
        val release = CompletableDeferred<Unit>()
        var creates = 0
        val fixture = Fixture(backgroundScope) {
            creates++
            release.await()
            AppResult.Success(EntryId("created"))
        }
        fixture.save()
        fixture.save()
        runCurrent()
        assertEquals(1, creates)
        assertEquals(1, fixture.starts)
        assertFalse(fixture.session.canEdit())
        release.complete(Unit)
        runCurrent()
        assertFalse(fixture.saving)
    }

    @Test
    fun lateSuccessCannotClearTheNewFormAfterReauthentication() = runTest {
        val release = CompletableDeferred<Unit>()
        val fixture = Fixture(backgroundScope) {
            withContext(NonCancellable) { release.await() }
            AppResult.Success(EntryId("created"))
        }
        val effects = mutableListOf<EditorSaveEffect>()
        backgroundScope.launch { fixture.session.effects.toList(effects) }
        fixture.save()
        runCurrent()
        fixture.access.lock()
        runCurrent()
        fixture.access.authenticate(2L)
        runCurrent()
        assertTrue(fixture.session.canEdit())
        fixture.form = "new form"
        release.complete(Unit)
        runCurrent()
        assertEquals("new form", fixture.form)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun lateFailureCannotResetTheNewAttempt() = runTest {
        val oldRelease = CompletableDeferred<Unit>()
        val newRelease = CompletableDeferred<Unit>()
        var creates = 0
        val fixture = Fixture(backgroundScope) {
            if (++creates == 1) {
                withContext(NonCancellable) { oldRelease.await() }
                AppResult.Failure(Conflict())
            } else {
                newRelease.await()
                AppResult.Success(EntryId("new"))
            }
        }
        fixture.save()
        runCurrent()
        fixture.session.clear()
        assertTrue(fixture.session.canEdit())
        fixture.form = "new form"
        fixture.save()
        runCurrent()
        oldRelease.complete(Unit)
        runCurrent()
        assertTrue(fixture.saving)
        assertEquals("new form", fixture.form)
        newRelease.complete(Unit)
        runCurrent()
        assertFalse(fixture.saving)
    }

    @Test
    fun changingTheAuthenticatedOwnerInvalidatesABufferedOutcome() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.save()
        runCurrent()
        fixture.access.authenticate(2L)
        val effects = mutableListOf<EditorSaveEffect>()
        backgroundScope.launch { fixture.session.effects.toList(effects) }
        runCurrent()
        assertTrue(effects.isEmpty())
    }

    @Test
    fun startingANewFormDropsThePreviousBufferedSuccess() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.save()
        runCurrent()
        assertTrue(fixture.session.canEdit())
        fixture.form = "new form"
        val effects = mutableListOf<EditorSaveEffect>()
        backgroundScope.launch { fixture.session.effects.toList(effects) }
        runCurrent()
        assertTrue(effects.isEmpty())
        assertEquals("new form", fixture.form)
    }

    @Test
    fun failureRestoresEditingAndRetryReplacesItsBufferedOutcome() = runTest {
        var creates = 0
        val fixture = Fixture(backgroundScope) {
            if (++creates == 1) AppResult.Failure(Conflict()) else AppResult.Success(EntryId("created"))
        }
        fixture.save()
        runCurrent()
        assertEquals("original", fixture.form)
        assertFalse(fixture.saving)
        fixture.save()
        runCurrent()
        val effects = mutableListOf<EditorSaveEffect>()
        backgroundScope.launch { fixture.session.effects.toList(effects) }
        runCurrent()
        assertEquals(listOf(EditorSaveEffect.Saved), effects)
        assertEquals("", fixture.form)
    }

    @Test
    fun cancellationRestoresIdleWithoutPublishingFailure() = runTest {
        val fixture = Fixture(backgroundScope) { throw CancellationException() }
        val effects = mutableListOf<EditorSaveEffect>()
        backgroundScope.launch { fixture.session.effects.toList(effects) }
        fixture.save()
        runCurrent()
        assertFalse(fixture.saving)
        assertEquals("original", fixture.form)
        assertTrue(fixture.session.canEdit())
        assertTrue(effects.isEmpty())
    }

    @Test
    fun unexpectedFailureUsesTheLocalizedFallbackWithoutExposingTheException() = runTest {
        val fixture = Fixture(backgroundScope) { error("synthetic private input") }
        val effects = mutableListOf<EditorSaveEffect>()
        backgroundScope.launch { fixture.session.effects.toList(effects) }
        fixture.save()
        runCurrent()
        assertEquals(listOf(EditorSaveEffect.Failed(null)), effects)
        assertFalse(fixture.saving)
        assertEquals("original", fixture.form)
    }

    @Test
    fun closedViewModelCannotBeReactivatedByAuthenticationOrInput() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.session.close()
        fixture.access.authenticate(2L)
        runCurrent()
        assertFalse(fixture.session.canEdit())
        fixture.save()
        runCurrent()
        assertEquals(0, fixture.starts)
        assertEquals("", fixture.form)
    }

    @Test
    fun lockedInputAndSaveAreRejectedBeforeBuildingADraft() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.access.lock()
        var drafts = 0
        assertFalse(fixture.session.canEdit())
        fixture.session.save { drafts++; draft() }
        runCurrent()
        assertEquals(0, drafts)
        assertEquals(0, fixture.starts)
        assertEquals("", fixture.form)
    }

    private class Access : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Authenticated(1L))
        override fun isUnlocked() = authenticationState.value is AuthenticationState.Authenticated
        fun lock() { authenticationState.value = AuthenticationState.Locked }
        fun authenticate(time: Long) { authenticationState.value = AuthenticationState.Authenticated(time) }
    }

    private class Fixture(
        scope: CoroutineScope,
        create: suspend (EntryDraft) -> AppResult<EntryId> = { AppResult.Success(EntryId("created")) },
    ) {
        val access = Access()
        var form = "original"
        var saving = false
        var starts = 0
        val session = EditorSaveSession(scope, access, create,
            onSaving = { saving = true; starts++ },
            onIdle = { saving = false },
            onClear = { form = ""; saving = false },
        )
        init { check(session.canEdit()) }
        fun save() = session.save { draft() }
    }

    private companion object {
        fun draft() = EntryDraft(EntryDraftTarget.New(EntryType.LOGIN))
    }
}
