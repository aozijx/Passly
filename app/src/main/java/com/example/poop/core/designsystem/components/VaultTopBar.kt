package com.example.poop.core.designsystem.components

import android.content.Intent
import android.provider.Settings
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.poop.R
import com.example.poop.core.common.VaultTab
import com.example.poop.features.vault.VaultViewModel
import com.example.poop.util.Logcat
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(
    vaultViewModel: VaultViewModel,
    scrollBehavior: TopAppBarScrollBehavior,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val searchQuery by vaultViewModel.searchQuery.collectAsState()
    val selectedCategory by vaultViewModel.selectedCategory.collectAsState()
    val availableCategories by vaultViewModel.availableCategories.collectAsState()
    val selectedTab by vaultViewModel.selectedTab.collectAsState()
    val focusManager = LocalFocusManager.current

    val autofillToastMessage = stringResource(R.string.vault_toast_enable_autofill_manual)
    var showCategorySubMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(vaultViewModel.isMoreMenuExpanded) {
        if (!vaultViewModel.isMoreMenuExpanded) showCategorySubMenu = false
    }

    LaunchedEffect(vaultViewModel.isSearchActive) {
        if (vaultViewModel.isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Column {
        CenterAlignedTopAppBar(
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets.statusBars,
            title = {
                if (vaultViewModel.isSearchActive) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { vaultViewModel.onSearchQueryChange(it) },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        placeholder = { Text(stringResource(R.string.vault_search_placeholder)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (selectedCategory != null) stringResource(R.string.vault_title_category, selectedCategory!!) 
                                   else stringResource(R.string.vault_title_default),
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedCategory != null) {
                            IconButton(onClick = { vaultViewModel.selectedCategory.let { (vaultViewModel.selectedCategory as MutableStateFlow).value = null } }) {
                                Icon(Icons.Default.Clear, stringResource(R.string.vault_clear_filter), modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { vaultViewModel.toggleSearch(!vaultViewModel.isSearchActive) }) {
                    Icon(
                        if (vaultViewModel.isSearchActive) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Search,
                        contentDescription = stringResource(if (vaultViewModel.isSearchActive) R.string.action_back else R.string.action_search)
                    )
                }
            },
            actions = {
                if (!vaultViewModel.isSearchActive) {
                    Box {
                        IconButton(onClick = { vaultViewModel.isMoreMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more))
                        }
                        DropdownMenu(
                            expanded = vaultViewModel.isMoreMenuExpanded,
                            onDismissRequest = { vaultViewModel.isMoreMenuExpanded = false }
                        ) {
                            if (!showCategorySubMenu) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.vault_menu_filter)) },
                                    onClick = { showCategorySubMenu = true },
                                    leadingIcon = { Icon(Icons.Default.FilterList, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (vaultViewModel.showTOTPCode) R.string.vault_menu_hide_totp else R.string.vault_menu_show_totp)) },
                                    onClick = { vaultViewModel.showTOTPCode = !vaultViewModel.showTOTPCode; vaultViewModel.isMoreMenuExpanded = false },
                                    leadingIcon = { Icon(if (vaultViewModel.showTOTPCode) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) }
                                )

                                val autofillManager = remember { context.getSystemService(AutofillManager::class.java) }
                                val isAutofillEnabled = remember(autofillManager) {
                                    autofillManager?.isAutofillSupported == true && autofillManager.hasEnabledAutofillServices()
                                }
                                if (!isAutofillEnabled) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.vault_menu_enable_autofill)) },
                                        onClick = {
                                            vaultViewModel.isMoreMenuExpanded = false
                                            val standardIntent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply { data = "package:${context.packageName}".toUri() }
                                            fun tryStartActivity(intent: Intent): Boolean {
                                                return try {
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    context.startActivity(intent)
                                                    true
                                                } catch (e: Exception) {
                                                    Logcat.e("VaultTopBar", "Failed to start activity", e)
                                                    false
                                                }
                                            }

                                            if (!tryStartActivity(standardIntent)) {
                                                if (!tryStartActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))) {
                                                    tryStartActivity(Intent(Settings.ACTION_SETTINGS))
                                                    Toast.makeText(context, autofillToastMessage, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Default.SettingsSuggest, null) }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_settings)) },
                                    onClick = { vaultViewModel.isMoreMenuExpanded = false; onSettingsClick() },
                                    leadingIcon = { Icon(Icons.Default.Settings, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.vault_menu_export)) },
                                    onClick = { vaultViewModel.isMoreMenuExpanded = false; onExportClick() },
                                    leadingIcon = { Icon(Icons.Default.FileUpload, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.vault_menu_import)) },
                                    onClick = { vaultViewModel.isMoreMenuExpanded = false; onImportClick() },
                                    leadingIcon = { Icon(Icons.Default.FileDownload, null) }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_back)) },
                                    onClick = { showCategorySubMenu = false },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.vault_menu_all_categories)) },
                                    onClick = { (vaultViewModel.selectedCategory as MutableStateFlow).value = null; vaultViewModel.isMoreMenuExpanded = false },
                                    trailingIcon = { if (selectedCategory == null) Icon(Icons.Default.Check, null) }
                                )
                                availableCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = { (vaultViewModel.selectedCategory as MutableStateFlow).value = category; vaultViewModel.isMoreMenuExpanded = false },
                                        trailingIcon = { if (selectedCategory == category) Icon(Icons.Default.Check, null) }
                                    )
                                }
                            }
                        }
                    }
                } else if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { vaultViewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.vault_clear_filter))
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            )
        )

        AnimatedVisibility(
            visible = !vaultViewModel.isSearchActive && selectedCategory == null && scrollBehavior.state.collapsedFraction < 0.5f,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                SecondaryTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(selectedTab.ordinal),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    VaultTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { (vaultViewModel.selectedTab as MutableStateFlow).value = tab },
                            text = {
                                Text(
                                    stringResource(when (tab) {
                                        VaultTab.ALL -> R.string.vault_tab_all
                                        VaultTab.PASSWORDS -> R.string.vault_tab_passwords
                                        VaultTab.TOTP -> R.string.vault_tab_totp
                                    }),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
