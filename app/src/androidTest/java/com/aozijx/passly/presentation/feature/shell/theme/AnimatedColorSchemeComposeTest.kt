package com.aozijx.passly.presentation.feature.shell.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnimatedColorSchemeComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstCompositionIsSettledAndRapidRetargetContinuesFromVisibleColor() {
        composeRule.mainClock.autoAdvance = false
        val first = lightColorScheme(primary = Color.Black)
        val second = lightColorScheme(primary = Color.White)
        val third = lightColorScheme(primary = Color.Red)
        val target = mutableStateOf(first)
        val observed = AtomicReference<ColorScheme>()

        composeRule.setContent {
            observed.set(
                rememberAnimatedColorScheme(
                    target = target.value,
                    animationSpec = tween(durationMillis = 1_000, easing = LinearEasing),
                ),
            )
        }
        composeRule.runOnIdle {
            assertEquals(first.primary, observed.get().primary)
            target.value = second
        }

        composeRule.mainClock.advanceTimeBy(400)
        val beforeRetarget = composeRule.runOnIdle {
            observed.get().primary.also { color ->
                assertNotEquals(first.primary, color)
                assertNotEquals(second.primary, color)
                target.value = third
            }
        }

        composeRule.mainClock.advanceTimeByFrame()
        composeRule.runOnIdle {
            val afterRetarget = observed.get().primary
            assertColorNear(beforeRetarget, afterRetarget, tolerance = 0.04f)
        }

        composeRule.mainClock.advanceTimeBy(1_100)
        composeRule.runOnIdle {
            assertEquals(third.primary, observed.get().primary)
        }
    }

    @Test
    fun returningToOriginalTargetRetargetsInsteadOfJumping() {
        composeRule.mainClock.autoAdvance = false
        val first = lightColorScheme(primary = Color.Black)
        val second = lightColorScheme(primary = Color.White)
        val target = mutableStateOf(first)
        val observed = AtomicReference<ColorScheme>()

        composeRule.setContent {
            observed.set(
                rememberAnimatedColorScheme(
                    target = target.value,
                    animationSpec = tween(durationMillis = 1_000, easing = LinearEasing),
                ),
            )
        }
        composeRule.runOnIdle { target.value = second }
        composeRule.mainClock.advanceTimeBy(400)
        val beforeReturn = composeRule.runOnIdle {
            observed.get().primary.also { target.value = first }
        }

        composeRule.mainClock.advanceTimeByFrame()
        composeRule.runOnIdle {
            assertColorNear(beforeReturn, observed.get().primary, tolerance = 0.04f)
        }
        composeRule.mainClock.advanceTimeBy(1_100)
        composeRule.runOnIdle {
            assertEquals(first.primary, observed.get().primary)
        }
    }

    private fun assertColorNear(expected: Color, actual: Color, tolerance: Float) {
        val midpoint = lerp(expected, actual, 0.5f)
        assertEquals(expected.red, midpoint.red, tolerance)
        assertEquals(expected.green, midpoint.green, tolerance)
        assertEquals(expected.blue, midpoint.blue, tolerance)
        assertEquals(expected.alpha, midpoint.alpha, tolerance)
    }
}
