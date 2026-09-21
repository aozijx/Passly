package com.aozijx.passly.presentation.feature.vault.editor.ui.common

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aozijx.passly.core.ui.animation.SharedTransitionOverlayClip
import com.aozijx.passly.core.ui.animation.withSharedTransitionVisualOverflow
import com.aozijx.passly.presentation.feature.vault.ui.ADD_ENTRY_FAB_SHARED_KEY
import com.aozijx.passly.presentation.feature.vault.ui.AddEntryFabVisualOverflow

@Composable
fun SharedTransitionScope.rememberAddEntryFabTransitionModifier(
    animatedVisibilityScope: AnimatedVisibilityScope,
): Modifier {
    val sharedModifier = Modifier.sharedBounds(
        sharedContentState = rememberSharedContentState(ADD_ENTRY_FAB_SHARED_KEY),
        animatedVisibilityScope = animatedVisibilityScope,
        resizeMode = RemeasureToBounds,
        clipInOverlayDuringTransition = SharedTransitionOverlayClip.None,
    )
    return Modifier.withSharedTransitionVisualOverflow(
        sharedModifier = sharedModifier,
        visualOverflow = AddEntryFabVisualOverflow,
    )
}
