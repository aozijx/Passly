package com.aozijx.passly.presentation.feature.shell.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp

internal fun ColorScheme.colorSchemeRoles(): List<Color> =
    listOf(
        primary, onPrimary, primaryContainer, onPrimaryContainer, inversePrimary,
        secondary, onSecondary, secondaryContainer, onSecondaryContainer,
        tertiary, onTertiary, tertiaryContainer, onTertiaryContainer,
        background, onBackground, surface, onSurface, surfaceVariant, onSurfaceVariant,
        surfaceTint, inverseSurface, inverseOnSurface, error, onError, errorContainer,
        onErrorContainer, outline, outlineVariant, scrim, surfaceBright, surfaceDim,
        surfaceContainer, surfaceContainerHigh, surfaceContainerHighest,
        surfaceContainerLow, surfaceContainerLowest, primaryFixed, primaryFixedDim,
        onPrimaryFixed, onPrimaryFixedVariant, secondaryFixed, secondaryFixedDim,
        onSecondaryFixed, onSecondaryFixedVariant, tertiaryFixed, tertiaryFixedDim,
        onTertiaryFixed, onTertiaryFixedVariant,
    )

internal fun ColorScheme.colorSchemeSignature(): List<Color> = colorSchemeRoles()

internal data class RetargetedColorScheme(
    val start: ColorScheme,
    val stop: ColorScheme,
)

internal fun retargetColorScheme(
    start: ColorScheme,
    stop: ColorScheme,
    progress: Float,
    newTarget: ColorScheme,
) = RetargetedColorScheme(
    start = lerpColorScheme(start, stop, progress),
    stop = newTarget,
)

internal fun lerpColorScheme(
    start: ColorScheme,
    stop: ColorScheme,
    fraction: Float,
): ColorScheme {
    val progress = fraction.coerceIn(0f, 1f)
    if (progress == 0f) return start
    if (progress == 1f) return stop
    fun role(from: Color, to: Color) = when {
        !from.isSpecified -> to
        !to.isSpecified -> from
        else -> lerp(from, to, progress)
    }

    return start.copy(
        primary = role(start.primary, stop.primary),
        onPrimary = role(start.onPrimary, stop.onPrimary),
        primaryContainer = role(start.primaryContainer, stop.primaryContainer),
        onPrimaryContainer = role(start.onPrimaryContainer, stop.onPrimaryContainer),
        inversePrimary = role(start.inversePrimary, stop.inversePrimary),
        secondary = role(start.secondary, stop.secondary),
        onSecondary = role(start.onSecondary, stop.onSecondary),
        secondaryContainer = role(start.secondaryContainer, stop.secondaryContainer),
        onSecondaryContainer = role(start.onSecondaryContainer, stop.onSecondaryContainer),
        tertiary = role(start.tertiary, stop.tertiary),
        onTertiary = role(start.onTertiary, stop.onTertiary),
        tertiaryContainer = role(start.tertiaryContainer, stop.tertiaryContainer),
        onTertiaryContainer = role(start.onTertiaryContainer, stop.onTertiaryContainer),
        background = role(start.background, stop.background),
        onBackground = role(start.onBackground, stop.onBackground),
        surface = role(start.surface, stop.surface),
        onSurface = role(start.onSurface, stop.onSurface),
        surfaceVariant = role(start.surfaceVariant, stop.surfaceVariant),
        onSurfaceVariant = role(start.onSurfaceVariant, stop.onSurfaceVariant),
        surfaceTint = role(start.surfaceTint, stop.surfaceTint),
        inverseSurface = role(start.inverseSurface, stop.inverseSurface),
        inverseOnSurface = role(start.inverseOnSurface, stop.inverseOnSurface),
        error = role(start.error, stop.error),
        onError = role(start.onError, stop.onError),
        errorContainer = role(start.errorContainer, stop.errorContainer),
        onErrorContainer = role(start.onErrorContainer, stop.onErrorContainer),
        outline = role(start.outline, stop.outline),
        outlineVariant = role(start.outlineVariant, stop.outlineVariant),
        scrim = role(start.scrim, stop.scrim),
        surfaceBright = role(start.surfaceBright, stop.surfaceBright),
        surfaceDim = role(start.surfaceDim, stop.surfaceDim),
        surfaceContainer = role(start.surfaceContainer, stop.surfaceContainer),
        surfaceContainerHigh = role(start.surfaceContainerHigh, stop.surfaceContainerHigh),
        surfaceContainerHighest = role(
            start.surfaceContainerHighest,
            stop.surfaceContainerHighest,
        ),
        surfaceContainerLow = role(start.surfaceContainerLow, stop.surfaceContainerLow),
        surfaceContainerLowest = role(
            start.surfaceContainerLowest,
            stop.surfaceContainerLowest,
        ),
        primaryFixed = role(start.primaryFixed, stop.primaryFixed),
        primaryFixedDim = role(start.primaryFixedDim, stop.primaryFixedDim),
        onPrimaryFixed = role(start.onPrimaryFixed, stop.onPrimaryFixed),
        onPrimaryFixedVariant = role(
            start.onPrimaryFixedVariant,
            stop.onPrimaryFixedVariant,
        ),
        secondaryFixed = role(start.secondaryFixed, stop.secondaryFixed),
        secondaryFixedDim = role(start.secondaryFixedDim, stop.secondaryFixedDim),
        onSecondaryFixed = role(start.onSecondaryFixed, stop.onSecondaryFixed),
        onSecondaryFixedVariant = role(
            start.onSecondaryFixedVariant,
            stop.onSecondaryFixedVariant,
        ),
        tertiaryFixed = role(start.tertiaryFixed, stop.tertiaryFixed),
        tertiaryFixedDim = role(start.tertiaryFixedDim, stop.tertiaryFixedDim),
        onTertiaryFixed = role(start.onTertiaryFixed, stop.onTertiaryFixed),
        onTertiaryFixedVariant = role(
            start.onTertiaryFixedVariant,
            stop.onTertiaryFixedVariant,
        ),
    )
}

@Composable
internal fun rememberAnimatedColorScheme(
    target: ColorScheme,
    animationSpec: FiniteAnimationSpec<Float>,
): ColorScheme {
    val targetSignature = remember(target) { target.colorSchemeSignature() }
    var appliedSignature by remember { mutableStateOf(targetSignature) }
    var start by remember { mutableStateOf(target) }
    var stop by remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(targetSignature) {
        if (targetSignature == appliedSignature) return@LaunchedEffect

        val retargeted = retargetColorScheme(start, stop, progress.value, target)
        start = retargeted.start
        stop = retargeted.stop
        appliedSignature = targetSignature
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec)
    }

    return lerpColorScheme(start, stop, progress.value)
}
