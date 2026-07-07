package com.aozijx.passly.ui.features.vault.components.topbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.model.SortOption

private enum class MenuPage { MAIN, SORT, FILTER }

@Composable
fun VaultDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    showTOTPCode: Boolean,
    onToggleTotpVisibility: () -> Unit,
    isAutofillEnabled: Boolean,
    onEnableAutofillClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExportClick: () -> Unit,
    onOpenPlainExport: () -> Unit,
    onImportClick: () -> Unit,
    availableCategories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit
) {
    var currentPage by remember { mutableStateOf(MenuPage.MAIN) }
    var categorySearchQuery by remember { mutableStateOf("") }
    var isCategorySearchVisible by remember { mutableStateOf(false) }
    val categoryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(expanded) {
        if (!expanded) {
            currentPage = MenuPage.MAIN
            categorySearchQuery = ""
            isCategorySearchVisible = false
        }
    }

    LaunchedEffect(isCategorySearchVisible) {
        if (isCategorySearchVisible) categoryFocusRequester.requestFocus()
    }

    val filteredCategories = remember(availableCategories, categorySearchQuery) {
        if (categorySearchQuery.isBlank()) availableCategories
        else availableCategories.filter { it.contains(categorySearchQuery, ignoreCase = true) }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .widthIn(min = 150.dp)
            .animateContentSize()
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                val enteringMain = targetState == MenuPage.MAIN
                val transition = if (enteringMain) {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                } else {
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                }
                transition.using(SizeTransform(clip = false))
            },
            label = "MenuPageTransition"
        ) { page ->
            Column(modifier = Modifier.fillMaxWidth()) {
                when (page) {
                    MenuPage.MAIN -> MainMenuContent(
                        onSortClick = { currentPage = MenuPage.SORT },
                        onFilterClick = { currentPage = MenuPage.FILTER },
                        showTOTPCode = showTOTPCode,
                        onToggleTotpVisibility = onToggleTotpVisibility,
                        onDismissRequest = onDismissRequest,
                        isAutofillEnabled = isAutofillEnabled,
                        onEnableAutofillClick = onEnableAutofillClick,
                        onSettingsClick = onSettingsClick,
                        onExportClick = onExportClick,
                        onOpenPlainExport = onOpenPlainExport,
                        onImportClick = onImportClick
                    )

                    MenuPage.SORT -> SortSubMenu(
                        selectedSort = selectedSort,
                        onSortSelected = { sort ->
                            onSortSelected(sort)
                            onDismissRequest()
                        },
                        onBack = { currentPage = MenuPage.MAIN }
                    )

                    MenuPage.FILTER -> FilterSubMenu(
                        isCategorySearchVisible = isCategorySearchVisible,
                        onToggleSearch = { isCategorySearchVisible = it },
                        categorySearchQuery = categorySearchQuery,
                        onCategorySearchQueryChange = { categorySearchQuery = it },
                        categoryFocusRequester = categoryFocusRequester,
                        filteredCategories = filteredCategories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { category ->
                            onCategorySelected(category)
                            onDismissRequest()
                        },
                        onBack = {
                            if (isCategorySearchVisible) {
                                isCategorySearchVisible = false
                                categorySearchQuery = ""
                            } else {
                                currentPage = MenuPage.MAIN
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainMenuContent(
    onSortClick: () -> Unit,
    onFilterClick: () -> Unit,
    showTOTPCode: Boolean,
    onToggleTotpVisibility: () -> Unit,
    onDismissRequest: () -> Unit,
    isAutofillEnabled: Boolean,
    onEnableAutofillClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExportClick: () -> Unit,
    onOpenPlainExport: () -> Unit,
    onImportClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.vault_menu_sort)) },
        onClick = onSortClick,
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) })
    DropdownMenuItem(
        text = { Text(stringResource(R.string.vault_menu_filter)) },
        onClick = onFilterClick,
        leadingIcon = { Icon(Icons.Default.FilterList, null) })
    DropdownMenuItem(
        text = {
            Text(
                stringResource(
                    if (showTOTPCode) R.string.vault_menu_hide_totp
                    else R.string.vault_menu_show_totp
                )
            )
        },
        onClick = { onToggleTotpVisibility(); onDismissRequest() },
        leadingIcon = {
            Icon(
                if (showTOTPCode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                null
            )
        })
    if (!isAutofillEnabled) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.vault_menu_enable_autofill)) },
            onClick = { onEnableAutofillClick(); onDismissRequest() },
            leadingIcon = { Icon(Icons.Default.SettingsSuggest, null) })
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    DropdownMenuItem(
        text = { Text(stringResource(R.string.settings)) },
        onClick = { onDismissRequest(); onSettingsClick() },
        leadingIcon = { Icon(Icons.Default.Settings, null) })
    CustomExportMenuItem(
        text = stringResource(R.string.vault_menu_export),
        leadingIcon = { Icon(Icons.Default.FileUpload, null) },
        onClick = { onDismissRequest(); onExportClick() },
        onLongClick = { onDismissRequest(); onOpenPlainExport() })
    DropdownMenuItem(
        text = { Text(stringResource(R.string.vault_menu_import)) },
        onClick = { onDismissRequest(); onImportClick() },
        leadingIcon = { Icon(Icons.Default.FileDownload, null) })
}

@Composable
private fun SortSubMenu(
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onBack: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.back)) },
        onClick = onBack,
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) })
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    SortOption.Companion.displayOptions().forEach { displayOption ->
        val isInGroup = selectedSort.group == displayOption.group
        val effectiveSort = if (isInGroup) selectedSort else displayOption
        val direction =
            if (displayOption.group == SortOption.SortGroup.STANDALONE) "" else if (effectiveSort.isDescending) " \u2193" else " \u2191"

        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(effectiveSort.labelResId) + direction,
                    color = if (isInGroup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isInGroup) FontWeight.Bold else FontWeight.Normal
                )
            },
            onClick = {
                if (isInGroup) {
                    onSortSelected(selectedSort.toggled())
                } else {
                    onSortSelected(displayOption)
                }
            },
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .clip(MaterialTheme.shapes.small)
                .background(if (isInGroup) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        )
    }
}

@Composable
private fun FilterSubMenu(
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
                if (isCategorySearchVisible) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                null
            )
        })

    AnimatedVisibility(visible = isCategorySearchVisible) {
        OutlinedTextField(
            value = categorySearchQuery,
            onValueChange = onCategorySearchQueryChange,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .fillMaxWidth()
                .focusRequester(categoryFocusRequester),
            placeholder = {
                Text(
                    "搜索分类...", style = MaterialTheme.typography.bodySmall
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search, null, modifier = Modifier.size(16.dp)
                )
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.3f
                )
            )
        )
    }

    if (!isCategorySearchVisible) {
        DropdownMenuItem(
            text = { Text("搜索分类") },
            onClick = { onToggleSearch(true) },
            leadingIcon = { Icon(Icons.Default.Search, null) })
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    val isAllSelected = selectedCategory == null
    DropdownMenuItem(
        text = {
            Text(
                text = stringResource(R.string.vault_menu_all_categories),
                color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        onClick = { onCategorySelected(null) },
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (isAllSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
    )

    filteredCategories.forEach { category ->
        val isSelected = selectedCategory == category
        DropdownMenuItem(
            text = {
                Text(
                    text = category,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            },
            onClick = { onCategorySelected(category) },
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .clip(MaterialTheme.shapes.small)
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        )
    }
}