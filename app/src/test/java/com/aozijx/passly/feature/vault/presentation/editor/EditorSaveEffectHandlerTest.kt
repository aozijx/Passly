package com.aozijx.passly.feature.vault.presentation.editor

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorSaveEffectHandlerTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `slow snackbar cleanup cannot postpone successful navigation`() = runTest {
        val owner = Owner()
        owner.lifecycle.currentState = Lifecycle.State.STARTED
        val channel = Channel<EditorSaveEffect>(Channel.CONFLATED)
        val cleanup = CompletableDeferred<Unit>()
        var saved = false
        backgroundScope.launch {
            collectEditorSaveEffects(owner.lifecycle, channel.receiveAsFlow(), onFailure = {
                try { awaitCancellation() } finally { withContext(NonCancellable) { cleanup.await() } }
            }, onSaved = { saved = true })
        }
        channel.send(EditorSaveEffect.Failed)
        runCurrent()
        channel.send(EditorSaveEffect.Saved)
        runCurrent()
        // Release native/UI cleanup before assertions so a failure cannot leak a waiting coroutine.
        val navigatedBeforeCleanup = saved
        cleanup.complete(Unit)
        runCurrent()
        assertEquals(true, navigatedBeforeCleanup)
    }

    @Test
    fun `save feedback waits for a started page and resumes once`() = runTest {
        val owner = Owner()
        owner.lifecycle.currentState = Lifecycle.State.CREATED
        val channel = Channel<EditorSaveEffect>(Channel.CONFLATED)
        val handled = mutableListOf<String>()
        backgroundScope.launch {
            collectEditorSaveEffects(owner.lifecycle, channel.receiveAsFlow(),
                onFailure = { handled += "failure" }, onSaved = { handled += "saved" })
        }
        channel.send(EditorSaveEffect.Saved)
        runCurrent()
        assertEquals(emptyList<String>(), handled)
        owner.lifecycle.currentState = Lifecycle.State.STARTED
        runCurrent()
        assertEquals(listOf("saved"), handled)
        owner.lifecycle.currentState = Lifecycle.State.CREATED
        owner.lifecycle.currentState = Lifecycle.State.STARTED
        runCurrent()
        assertEquals(listOf("saved"), handled)
    }

    @Test
    fun `successful retry cancels pending failure snackbar without blocking navigation`() = runTest {
        val owner = Owner()
        owner.lifecycle.currentState = Lifecycle.State.STARTED
        val channel = Channel<EditorSaveEffect>(Channel.CONFLATED)
        val handled = mutableListOf<String>()
        backgroundScope.launch {
            collectEditorSaveEffects(owner.lifecycle, channel.receiveAsFlow(), onFailure = {
                handled += "snackbar"
                try { awaitCancellation() } finally { handled += "dismissed" }
            }, onSaved = { handled += "saved" })
        }
        channel.send(EditorSaveEffect.Failed)
        runCurrent()
        assertEquals(listOf("snackbar"), handled)
        channel.send(EditorSaveEffect.Saved)
        runCurrent()
        assertEquals(setOf("snackbar", "dismissed", "saved"), handled.toSet())
        assertEquals(3, handled.size)
    }

    @Test
    fun `stopping page dismisses snackbar and defers new feedback`() = runTest {
        val owner = Owner()
        owner.lifecycle.currentState = Lifecycle.State.STARTED
        val channel = Channel<EditorSaveEffect>(Channel.CONFLATED)
        val handled = mutableListOf<String>()
        backgroundScope.launch {
            collectEditorSaveEffects(owner.lifecycle, channel.receiveAsFlow(), onFailure = {
                handled += "snackbar"
                try { awaitCancellation() } finally { handled += "dismissed" }
            }, onSaved = { handled += "saved" })
        }
        channel.send(EditorSaveEffect.Failed)
        runCurrent()
        owner.lifecycle.currentState = Lifecycle.State.CREATED
        runCurrent()
        channel.send(EditorSaveEffect.Saved)
        runCurrent()
        assertEquals(listOf("snackbar", "dismissed"), handled)
        owner.lifecycle.currentState = Lifecycle.State.STARTED
        runCurrent()
        assertEquals(listOf("snackbar", "dismissed", "saved"), handled)
    }

    private class Owner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this)
    }
}
