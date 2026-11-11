package com.aozijx.passly.feature.scanner.presentation.integration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.aozijx.passly.feature.scanner.presentation.route.ScannerRoute

@Composable
internal fun ScannerOverlay(
    visible: Boolean,
    resolveResult: (String) -> ScannerResultPlan,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter =
            fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                slideInVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    initialOffsetY = { it },
                ),
        exit =
            fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                slideOutVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    targetOffsetY = { it },
                ),
    ) {
        ScannerRoute(
            visible = visible,
            resolveResult = resolveResult,
            onDismiss = onDismiss,
        )
    }
}
