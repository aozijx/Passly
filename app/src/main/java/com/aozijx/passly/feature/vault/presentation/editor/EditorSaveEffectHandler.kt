package com.aozijx.passly.feature.vault.presentation.editor

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal sealed interface EditorSaveEffect {
    data object Saved : EditorSaveEffect
    data object Failed : EditorSaveEffect
}

@Composable
internal fun EditorSaveEffectHandler(
    effects: Flow<EditorSaveEffect>,
    snackbarHostState: SnackbarHostState,
    defaultFailureMessage: String,
    onSaved: () -> Unit,
) {
    val currentOnSaved = rememberUpdatedState(onSaved)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(effects, lifecycleOwner, snackbarHostState, defaultFailureMessage) {
        collectEditorSaveEffects(
            lifecycle = lifecycleOwner.lifecycle,
            effects = effects,
            onFailure = { snackbarHostState.showSnackbar(defaultFailureMessage) },
            onSaved = { currentOnSaved.value() },
        )
    }
}

private sealed interface FailureNotice {
    data object Idle : FailureNotice
    class Showing(val job: Job) : FailureNotice
}

/** Keep result consumption synchronous; a transient notice must never hold up navigation. */
internal suspend fun collectEditorSaveEffects(
    lifecycle: Lifecycle,
    effects: Flow<EditorSaveEffect>,
    onFailure: suspend () -> Unit,
    onSaved: () -> Unit,
) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        var notice: FailureNotice = FailureNotice.Idle
        fun dismissFailure() {
            val previous = notice
            notice = FailureNotice.Idle
            if (previous is FailureNotice.Showing) previous.job.cancel()
        }
        try {
            effects.collect { effect ->
                dismissFailure()
                when (effect) {
                    EditorSaveEffect.Saved -> onSaved()
                    EditorSaveEffect.Failed -> notice = FailureNotice.Showing(launch { onFailure() })
                }
            }
        } finally {
            dismissFailure()
        }
    }
}
