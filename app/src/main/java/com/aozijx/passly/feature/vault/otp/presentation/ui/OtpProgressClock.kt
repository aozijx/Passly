package com.aozijx.passly.feature.vault.otp.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

private val defaultOtpProgressTime = mutableLongStateOf(System.currentTimeMillis())

internal val LocalOtpProgressTime =
    staticCompositionLocalOf<State<Long>> {
        defaultOtpProgressTime
    }

@Composable
internal fun ProvideOtpProgressClock(content: @Composable () -> Unit) {
    val progressTime = remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val nowMillis = System.currentTimeMillis()
                progressTime.longValue = nowMillis
                delay(1000L - nowMillis % 1000L)
            }
        }
    }

    CompositionLocalProvider(LocalOtpProgressTime provides progressTime, content = content)
}
