package com.aozijx.passly.presentation.shared.event

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PageEffectLifecycleTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `page effect waits until started and is delivered once after same visit resumes`() =
        runTest {
            val pageEffects = PageEffects<String>()
            val pageOwner = pageEffects.enter()
            val lifecycleOwner = Owner().apply { lifecycle.currentState = Lifecycle.State.CREATED }
            val received = mutableListOf<String>()
            backgroundScope.launch {
                collectPageEffects(lifecycleOwner.lifecycle, pageEffects.effects, received::add)
            }
            runCurrent()

            pageEffects.tryEmit(pageOwner, "save completed")
            runCurrent()
            assertEquals(emptyList<String>(), received)

            lifecycleOwner.lifecycle.currentState = Lifecycle.State.STARTED
            runCurrent()
            assertEquals(listOf("save completed"), received)

            lifecycleOwner.lifecycle.currentState = Lifecycle.State.CREATED
            runCurrent()
            pageEffects.tryEmit(pageOwner, "export picker")
            runCurrent()
            assertEquals(listOf("save completed"), received)

            lifecycleOwner.lifecycle.currentState = Lifecycle.State.STARTED
            runCurrent()
            lifecycleOwner.lifecycle.currentState = Lifecycle.State.CREATED
            runCurrent()
            lifecycleOwner.lifecycle.currentState = Lifecycle.State.STARTED
            runCurrent()
            assertEquals(listOf("save completed", "export picker"), received)
        }

    @Test
    fun `page exit drops effects buffered while stopped`() =
        runTest {
            val pageEffects = PageEffects<String>()
            val oldOwner = pageEffects.enter()
            val lifecycleOwner = Owner().apply { lifecycle.currentState = Lifecycle.State.CREATED }
            val received = mutableListOf<String>()
            backgroundScope.launch {
                collectPageEffects(lifecycleOwner.lifecycle, pageEffects.effects, received::add)
            }
            runCurrent()
            pageEffects.tryEmit(oldOwner, "old page")
            pageEffects.exit(oldOwner)
            val currentOwner = pageEffects.enter()
            pageEffects.tryEmit(currentOwner, "current page")

            lifecycleOwner.lifecycle.currentState = Lifecycle.State.STARTED
            runCurrent()

            assertEquals(listOf("current page"), received)
        }

    private class Owner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this)
    }
}
