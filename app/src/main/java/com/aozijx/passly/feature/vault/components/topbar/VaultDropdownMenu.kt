package com.aozijx.passly.feature.vault.components.topbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.aozijx.passly.domain.settings.model.VaultSortSpec
import com.aozijx.passly.domain.entry.model.EntryType

private enum class MenuPage { MAIN, SORT, FILTER }

@Composable
fun VaultDropdownMenu(
    onDismissRequest: () -> Unit,
    showTOTPCode: Boolean,
    onToggleTotpVisibility: () -> Unit,
    onSettingsClick: () -> Unit,
    availableEntryTypes: List<String>,
    selectedEntryTypeName: String?,
    onEntryTypeSelected: (String?) -> Unit,
    selectedSort: VaultSortSpec,
    onSortSelected: (VaultSortSpec) -> Unit
) {
    var currentPage by remember { mutableStateOf(MenuPage.MAIN) }
    var entryTypeSearchQuery by remember { mutableStateOf("") }
    var entryTypeSearchVisible by remember { mutableStateOf(false) }
    val entryTypeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(entryTypeSearchVisible) {
        if (entryTypeSearchVisible) entryTypeFocusRequester.requestFocus()
    }

    val filteredEntryTypes = remember(availableEntryTypes, entryTypeSearchQuery) {
        if (entryTypeSearchQuery.isBlank()) availableEntryTypes
        else availableEntryTypes.filter {
            it.contains(entryTypeSearchQuery, ignoreCase = true) ||
                EntryType.fromName(it).displayName.contains(
                    entryTypeSearchQuery,
                    ignoreCase = true
                )
        }
    }

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .widthIn(min = 150.dp)
            .animateContentSize()
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                val transition = if (targetState == MenuPage.MAIN) {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
                } else {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
                }
                transition.using(SizeTransform(clip = false))
            },
            label = "MenuPageTransition"
        ) { page ->
            Column(Modifier.fillMaxWidth()) {
                when (page) {
                    MenuPage.MAIN -> MainMenuContent(
                        onSortClick = { currentPage = MenuPage.SORT },
                        onFilterClick = { currentPage = MenuPage.FILTER },
                        showTOTPCode = showTOTPCode,
                        onToggleTotpVisibility = onToggleTotpVisibility,
                        onDismissRequest = onDismissRequest,
                        onSettingsClick = onSettingsClick
                    )
                    MenuPage.SORT -> SortSubMenu(
                        selectedSort = selectedSort,
                        onSortSelected = onSortSelected,
                        onBack = { currentPage = MenuPage.MAIN }
                    )
                    MenuPage.FILTER -> FilterSubMenu(
                        isEntryTypeSearchVisible = entryTypeSearchVisible,
                        onToggleSearch = { entryTypeSearchVisible = it },
                        entryTypeSearchQuery = entryTypeSearchQuery,
                        onEntryTypeSearchQueryChange = { entryTypeSearchQuery = it },
                        entryTypeFocusRequester = entryTypeFocusRequester,
                        filteredEntryTypes = filteredEntryTypes,
                        selectedEntryTypeName = selectedEntryTypeName,
                        onEntryTypeSelected = {
                            onEntryTypeSelected(it)
                            onDismissRequest()
                        },
                        onBack = {
                            if (entryTypeSearchVisible) {
                                entryTypeSearchVisible = false
                                entryTypeSearchQuery = ""
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
