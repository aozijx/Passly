package com.aozijx.passly.presentation.feature.vault.editor

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow

internal sealed interface EditorSaveEffect {
    data object Saved : EditorSaveEffect
    data class Failed(val message: String?) : EditorSaveEffect
}

@Composable
internal fun EditorSaveEffectHandler(
    effects: Flow<EditorSaveEffect>,
    snackbarHostState: SnackbarHostState,
    defaultFailureMessage: String,
    onSaved: () -> Unit,
) {
    val currentOnSaved = rememberUpdatedState(onSaved)
    LaunchedEffect(effects, snackbarHostState, defaultFailureMessage) {
        effects.collect { effect ->
            when (effect) {
                EditorSaveEffect.Saved -> currentOnSaved.value()
                is EditorSaveEffect.Failed -> snackbarHostState.showSnackbar(
                    effect.message ?: defaultFailureMessage,
                )
            }
        }
    }
}
