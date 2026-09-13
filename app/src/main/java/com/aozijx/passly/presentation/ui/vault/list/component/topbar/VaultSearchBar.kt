package com.aozijx.passly.presentation.ui.vault.list.component.topbar

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VaultSearchBar(
    query: String,
    mode: VaultSearchBarMode,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onEditingChange: (Boolean) -> Unit,
    trailingIcon: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val textFieldState = rememberTextFieldState(query)
    val searchBarState = rememberSearchBarState()
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)
    val currentOnEditingChange by rememberUpdatedState(onEditingChange)

    LaunchedEffect(query) {
        if (textFieldState.text.toString() != query) {
            textFieldState.edit { replace(0, length, query) }
        }
    }
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .collect(currentOnQueryChange)
    }
    LaunchedEffect(mode) {
        if (mode == VaultSearchBarMode.EDITING) {
            searchBarState.animateToExpanded()
            focusRequester.requestFocus()
        } else {
            searchBarState.animateToCollapsed()
        }
    }
    LaunchedEffect(isFocused) {
        currentOnEditingChange(isFocused)
    }

    SearchBarDefaults.InputField(
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onSearch = onSearch,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = { Text(stringResource(R.string.vault_search_placeholder)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.search),
            )
        },
        trailingIcon = trailingIcon,
        interactionSource = interactionSource,
    )
}
