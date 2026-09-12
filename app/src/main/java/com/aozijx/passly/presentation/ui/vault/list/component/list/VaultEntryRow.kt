package com.aozijx.passly.presentation.ui.vault.list.component.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.core.ui.components.widgets.SwipeActionContainer
import com.aozijx.passly.core.ui.components.widgets.SwipeActionSpec
import com.aozijx.passly.presentation.ui.shared.gesture.SwipeActionUiModel
import com.aozijx.passly.presentation.ui.vault.list.component.cardstyle.CardStyleRegistry
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListContentUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemEventHandler
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpStateProvider
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpUiState
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun VaultEntryRow(
    item: VaultListItemUiModel,
    eventHandler: VaultListItemEventHandler,
    content: VaultListContentUiModel,
    otpStateProvider: VaultOtpStateProvider,
    animateInitialAppearance: Boolean,
    modifier: Modifier = Modifier,
) {
    val totpState = if (item.hasOtp) {
        observeVaultOtpState(
            entryId = item.id,
            provider = otpStateProvider,
            enabled = content.showTotpCode,
        )
    } else {
        null
    }
    val cardStyle = remember(
        item.entryType,
        item.hasPassword,
        item.hasOtp,
        content.cardPresentations,
    ) {
        CardStyleRegistry.resolveStyle(item, content.cardPresentations)
    }
    val colorScheme = MaterialTheme.colorScheme
    val motionScheme = MaterialTheme.motionScheme
    val visibleState = remember(item.id) {
        MutableTransitionState(!animateInitialAppearance).apply { targetState = true }
    }
    val currentItem by rememberUpdatedState(item)
    val leftAction = remember(item.id, content.swipeLeftAction, eventHandler, colorScheme) {
        createAppSwipeActionSpec(
            actionType = content.swipeLeftAction,
            onAction = { eventHandler.onSwipe(currentItem, content.swipeLeftAction) },
            backgroundColor = if (content.swipeLeftAction == SwipeActionUiModel.DELETE) {
                colorScheme.error
            } else {
                colorScheme.primary
            },
            iconTint = Color.White,
        )
    }
    val rightAction = remember(item.id, content.swipeRightAction, eventHandler, colorScheme) {
        createAppSwipeActionSpec(
            actionType = content.swipeRightAction,
            onAction = { eventHandler.onSwipe(currentItem, content.swipeRightAction) },
            backgroundColor = if (content.swipeRightAction == SwipeActionUiModel.DELETE) {
                colorScheme.error
            } else {
                colorScheme.secondary
            },
            iconTint = Color.White,
        )
    }

    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = fadeIn(animationSpec = motionScheme.fastEffectsSpec()) +
                slideInVertically(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    initialOffsetY = { height -> height / 4 },
                ),
    ) {
        SwipeActionContainer(
            leftAction = leftAction,
            rightAction = rightAction,
            modifier = Modifier.fillMaxWidth(),
            enabled = content.isSwipeEnabled,
        ) {
            cardStyle.Render(
                entry = item,
                totpState = totpState,
                showTotpCode = content.showTotpCode,
                onClick = { eventHandler.onClick(item) },
            )
        }
    }
}

@Composable
internal fun observeVaultOtpState(
    entryId: String,
    provider: VaultOtpStateProvider,
    enabled: Boolean,
): VaultOtpUiState? {
    if (!enabled) return null
    DisposableEffect(entryId, provider) {
        provider.subscribe(entryId)
        onDispose { provider.unsubscribe(entryId) }
    }
    val stateFlow = remember(entryId, provider) {
        provider.state(entryId).distinctUntilChanged()
    }
    val current by stateFlow.collectAsStateWithLifecycle(initialValue = null)
    return current
}

private fun createAppSwipeActionSpec(
    actionType: SwipeActionUiModel,
    onAction: () -> Unit,
    backgroundColor: Color,
    iconTint: Color,
) = SwipeActionSpec(
    icon = when (actionType) {
        SwipeActionUiModel.DELETE -> Icons.Default.Delete
        SwipeActionUiModel.DETAIL -> Icons.Default.Info
        SwipeActionUiModel.COPY_PASSWORD -> Icons.Default.ContentCopy
        SwipeActionUiModel.COPY_USERNAME -> Icons.Default.Person
    },
    backgroundColor = backgroundColor,
    iconTint = iconTint,
    onAction = onAction,
)
