package com.aozijx.passly.feature.vault.presentation.editor.ui.common

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aozijx.passly.core.ui.animation.SharedTransitionOverlayClip
import com.aozijx.passly.core.ui.animation.withSharedTransitionVisualOverflow
import com.aozijx.passly.feature.vault.presentation.ui.ADD_ENTRY_FAB_SHARED_KEY
import com.aozijx.passly.feature.vault.presentation.ui.AddEntryFabVisualOverflow

@Composable
fun Modifier.rememberAddEntryFabTransitionModifier(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
): Modifier {
    val sharedModifier = with(sharedTransitionScope) {
        Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(ADD_ENTRY_FAB_SHARED_KEY),
            animatedVisibilityScope = animatedVisibilityScope,
            resizeMode = RemeasureToBounds,
            clipInOverlayDuringTransition = SharedTransitionOverlayClip.None,
        )
    }
    return withSharedTransitionVisualOverflow(
        sharedModifier = sharedModifier,
        visualOverflow = AddEntryFabVisualOverflow,
    )
}
