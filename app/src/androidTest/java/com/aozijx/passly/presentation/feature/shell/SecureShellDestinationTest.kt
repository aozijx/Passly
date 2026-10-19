package com.aozijx.passly.presentation.feature.shell

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureShellDestinationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun leavingVaultDisposesSensitiveContentWithoutWaitingForAnimation() {
        val destination = mutableStateOf(AppShellDestination.VAULT)
        var vaultContentDisposed = false

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SecureShellDestination(destination.value) { currentDestination ->
                if (currentDestination == AppShellDestination.VAULT) {
                    DisposableEffect(Unit) {
                        onDispose { vaultContentDisposed = true }
                    }
                }
            }
        }

        composeRule.runOnIdle {
            destination.value = AppShellDestination.AUTHENTICATION
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.runOnIdle {
            assertTrue(vaultContentDisposed)
        }
    }
}
