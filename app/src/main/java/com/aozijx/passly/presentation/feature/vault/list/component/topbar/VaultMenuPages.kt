package com.aozijx.passly.presentation.feature.vault.list.component.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.menu.MenuOptionText
import com.aozijx.passly.core.ui.components.menu.selectedMenuModifier
import com.aozijx.passly.domain.entry.model.query.EntrySort

@Composable
internal fun MainMenuContent(
    onSortClick: () -> Unit,
    onCategoryFilterClick: () -> Unit,
    showTOTPCode: Boolean,
    onToggleTotpVisibility: () -> Unit,
    onDismissRequest: () -> Unit,
    onSettingsClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.vault_menu_sort)) },
        onClick = onSortClick,
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) }
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.vault_menu_filter)) },
        onClick = onCategoryFilterClick,
        leadingIcon = { Icon(Icons.Default.FilterList, null) }
    )
    DropdownMenuItem(
        text = {
            Text(
                stringResource(
                    if (showTOTPCode) R.string.vault_menu_hide_totp
                    else R.string.vault_menu_show_totp
                )
            )
        },
        onClick = {
            onToggleTotpVisibility()
            onDismissRequest()
        },
        leadingIcon = {
            Icon(
                if (showTOTPCode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                null
            )
        }
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    DropdownMenuItem(
        text = { Text(stringResource(R.string.settings_title)) },
        onClick = {
            onDismissRequest()
            onSettingsClick()
        },
        leadingIcon = { Icon(Icons.Default.Settings, null) }
    )
}

@Composable
internal fun SortSubMenu(
    selectedSort: EntrySort,
    onSortSelected: (EntrySort) -> Unit,
    onBack: () -> Unit
) {
    BackMenuItem(onBack)
    val isDefault = selectedSort == EntrySort.DEFAULT
    EntrySort.presets().forEach { preset ->
        val selected = preset.field == selectedSort.field
        val direction = when {
            preset == EntrySort.DEFAULT -> ""
            selected && !isDefault -> if (selectedSort.direction.name == "DESC") " \u2193" else " \u2191"
            else -> ""
        }
        DropdownMenuItem(
            text = {
                MenuOptionText(
                    text = stringResource(preset.labelResId()) + direction,
                    selected = selected
                )
            },
            onClick = {
                onSortSelected(if (selected && !isDefault) selectedSort.toggled() else preset)
            },
            modifier = Modifier.selectedMenuModifier(selected)
        )
    }
}

@Composable
internal fun FilterSubMenu(
    searchLabelRes: Int,
    searchHintRes: Int,
    isSearchVisible: Boolean,
    onToggleSearch: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    items: List<String>,
    selectedItem: String?,
    itemText: (String) -> String,
    onItemSelected: (String?) -> Unit,
    onBack: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.back)) },
        onClick = onBack,
        leadingIcon = {
            Icon(
                if (isSearchVisible) Icons.Default.Close
                else Icons.AutoMirrored.Filled.ArrowBack,
                null
            )
        }
    )
    AnimatedVisibility(isSearchVisible) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = {
                Text(
                    stringResource(searchHintRes),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp))
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        )
    }
    if (!isSearchVisible) {
        DropdownMenuItem(
            text = { Text(stringResource(searchLabelRes)) },
            onClick = { onToggleSearch(true) },
            leadingIcon = { Icon(Icons.Default.Search, null) }
        )
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    EntryTypeMenuItem(
        text = stringResource(R.string.tab_all),
        selected = selectedItem == null,
        onClick = { onItemSelected(null) }
    )
    items.forEach { item ->
        EntryTypeMenuItem(
            text = itemText(item),
            selected = selectedItem == item,
            onClick = { onItemSelected(item) }
        )
    }
}

@Composable
private fun BackMenuItem(onBack: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.back)) },
        onClick = onBack,
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun EntryTypeMenuItem(text: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            MenuOptionText(
                text = text,
                selected = selected
            )
        },
        onClick = onClick,
        modifier = Modifier.selectedMenuModifier(selected)
    )
}
