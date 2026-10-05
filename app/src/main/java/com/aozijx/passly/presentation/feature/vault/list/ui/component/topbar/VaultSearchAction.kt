package com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.list.VaultUiAction
import com.aozijx.passly.presentation.feature.vault.list.ui.search.VaultSearchPhase
import com.aozijx.passly.presentation.feature.vault.list.ui.search.VaultSearchStateHolder

private enum class VaultSearchActionType { MORE, CLEAR, NONE }

@Composable
internal fun VaultSearchAction(
    uiState: VaultTopBarUiState,
    searchStateHolder: VaultSearchStateHolder,
    onAction: (VaultUiAction) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }
    val action = when {
        searchStateHolder.uiState.phase == VaultSearchPhase.BROWSING ||
            searchStateHolder.uiState.phase == VaultSearchPhase.PULLING -> {
            VaultSearchActionType.MORE
        }
        uiState.query.isNotEmpty() -> VaultSearchActionType.CLEAR
        else -> VaultSearchActionType.NONE
    }

    LaunchedEffect(action) {
        if (action != VaultSearchActionType.MORE) moreMenuExpanded = false
    }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { moreMenuExpanded = false }
    }

    Box(modifier = modifier.zIndex(1f)) {
        when (action) {
            VaultSearchActionType.MORE -> IconButton(
                onClick = { moreMenuExpanded = true },
            ) {
                Icon(Icons.Default.MoreVert, stringResource(R.string.more))
            }
            VaultSearchActionType.CLEAR -> IconButton(
                onClick = searchStateHolder::clear,
            ) {
                Icon(
                    Icons.Default.Clear,
                    stringResource(R.string.vault_clear_filter),
                )
            }
            VaultSearchActionType.NONE -> Spacer(Modifier.size(VaultSearchActionSize))
        }
        VaultDropdownMenu(
            expanded = moreMenuExpanded,
            onDismissRequest = { moreMenuExpanded = false },
            uiState = uiState.menu,
            onAction = onAction,
            onSettingsClick = onSettingsClick,
        )
    }
}
