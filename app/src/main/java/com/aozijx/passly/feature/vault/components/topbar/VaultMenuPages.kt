package com.aozijx.passly.feature.vault.components.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.model.settings.SortOption

@Composable
internal fun MainMenuContent(
    onSortClick: () -> Unit,
    onFilterClick: () -> Unit,
    showTOTPCode: Boolean,
    onToggleTotpVisibility: () -> Unit,
    onDismissRequest: () -> Unit,
    onSettingsClick: () -> Unit,
    onExportClick: () -> Unit,
    onOpenPlainExport: () -> Unit,
    onImportClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.vault_menu_sort)) },
        onClick = onSortClick,
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) }
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.vault_menu_filter)) },
        onClick = onFilterClick,
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
        text = { Text(stringResource(R.string.settings)) },
        onClick = {
            onDismissRequest()
            onSettingsClick()
        },
        leadingIcon = { Icon(Icons.Default.Settings, null) }
    )
    CustomExportMenuItem(
        text = stringResource(R.string.vault_menu_export),
        leadingIcon = { Icon(Icons.Default.FileUpload, null) },
        onClick = {
            onDismissRequest()
            onExportClick()
        },
        onLongClick = {
            onDismissRequest()
            onOpenPlainExport()
        }
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.vault_menu_import)) },
        onClick = {
            onDismissRequest()
            onImportClick()
        },
        leadingIcon = { Icon(Icons.Default.FileDownload, null) }
    )
}

@Composable
internal fun SortSubMenu(
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onBack: () -> Unit
) {
    BackMenuItem(onBack)
    SortOption.displayOptions().forEach { displayOption ->
        val selected = selectedSort.group == displayOption.group
        val effective = if (selected) selectedSort else displayOption
        val direction = when {
            displayOption.group == SortOption.SortGroup.STANDALONE -> ""
            effective.isDescending -> " \u2193"
            else -> " \u2191"
        }
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(effective.labelResId()) + direction,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            },
            onClick = {
                onSortSelected(if (selected) selectedSort.toggled() else displayOption)
            },
            modifier = selectedMenuModifier(selected)
        )
    }
}

@Composable
internal fun FilterSubMenu(
    isCategorySearchVisible: Boolean,
    onToggleSearch: (Boolean) -> Unit,
    categorySearchQuery: String,
    onCategorySearchQueryChange: (String) -> Unit,
    categoryFocusRequester: FocusRequester,
    filteredCategories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    onBack: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.back)) },
        onClick = onBack,
        leadingIcon = {
            Icon(
                if (isCategorySearchVisible) Icons.Default.Close
                else Icons.AutoMirrored.Filled.ArrowBack,
                null
            )
        }
    )
    AnimatedVisibility(isCategorySearchVisible) {
        OutlinedTextField(
            value = categorySearchQuery,
            onValueChange = onCategorySearchQueryChange,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .fillMaxWidth()
                .focusRequester(categoryFocusRequester),
            placeholder = { Text("搜索分类...", style = MaterialTheme.typography.bodySmall) },
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
    if (!isCategorySearchVisible) {
        DropdownMenuItem(
            text = { Text("搜索分类") },
            onClick = { onToggleSearch(true) },
            leadingIcon = { Icon(Icons.Default.Search, null) }
        )
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    CategoryMenuItem(
        text = stringResource(R.string.vault_menu_all_categories),
        selected = selectedCategory == null,
        onClick = { onCategorySelected(null) }
    )
    filteredCategories.forEach { category ->
        CategoryMenuItem(
            text = category,
            selected = selectedCategory == category,
            onClick = { onCategorySelected(category) }
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
private fun CategoryMenuItem(text: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        onClick = onClick,
        modifier = selectedMenuModifier(selected)
    )
}

@Composable
private fun selectedMenuModifier(selected: Boolean): Modifier =
    Modifier
        .padding(horizontal = 8.dp, vertical = 2.dp)
        .clip(MaterialTheme.shapes.small)
        .background(
            if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        )
