package com.aozijx.passly.features.vault

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.designsystem.model.VaultCardStyle
import com.aozijx.passly.core.designsystem.widgets.EmptyVaultPlaceholder
import com.aozijx.passly.core.designsystem.widgets.SwipeDirection
import com.aozijx.passly.core.designsystem.widgets.SwipeToAction
import com.aozijx.passly.core.designsystem.widgets.createSwipeAction
import com.aozijx.passly.core.designsystem.widgets.handleSwipeAction
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.FieldKey
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.presentation.VaultSummary
import com.aozijx.passly.domain.strategy.EntryTypeStrategyFactory
import com.aozijx.passly.features.main.MainViewModel
import com.aozijx.passly.features.main.contract.MainIntent
import com.aozijx.passly.features.settings.SettingsViewModel
import com.aozijx.passly.features.vault.components.VaultDialogs
import com.aozijx.passly.features.vault.components.entries.VaultCardStyleRegistry
import com.aozijx.passly.features.vault.components.fab.VaultFab
import com.aozijx.passly.features.vault.components.topbar.VaultTopBar
import com.aozijx.passly.features.vault.model.VaultTab
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultContent(
    mainViewModel: MainViewModel,
    activity: FragmentActivity,
    vaultViewModel: VaultViewModel,
    settingsViewModel: SettingsViewModel,
    onSettingsClick: () -> Unit = {},
    onPlainExportClick: () -> Unit = {},
    onShowDetail: (VaultEntry) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by vaultViewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val authTitle = stringResource(R.string.auth_title)
    val decryptAuthTitle = stringResource(R.string.vault_auth_decrypt_title)
    val decryptAuthSubtitle = stringResource(R.string.vault_auth_decrypt_subtitle_generic)
    val totpCopiedText = stringResource(R.string.vault_totp_copied)
    val fieldCopiedFormat = stringResource(R.string.vault_field_copied_format)

    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val isSwipeEnabled = settingsUiState.isSwipeEnabled
    val swipeLeftAction = settingsUiState.swipeLeftAction
    val swipeRightAction = settingsUiState.swipeRightAction
    val isStatusBarAutoHide = settingsUiState.isStatusBarAutoHide
    val isTopBarCollapsible = settingsUiState.isTopBarCollapsible
    val isTabBarCollapsible = settingsUiState.isTabBarCollapsible
    val perTypeStyleMap = remember(settingsUiState.cardStyleByEntryType) {
        settingsUiState.cardStyleByEntryType
    }
    var isFabVisible by remember { mutableStateOf(true) }

    val initialTabIndex = uiState.visibleTabs.indexOf(uiState.selectedTab).coerceAtLeast(0)
    val pagerState =
        rememberPagerState(initialPage = initialTabIndex) { uiState.visibleTabs.size.coerceAtLeast(1) }

    LaunchedEffect(uiState.visibleTabs, uiState.selectedTab) {
        if (uiState.visibleTabs.isEmpty()) return@LaunchedEffect
        if (uiState.selectedTab !in uiState.visibleTabs) {
            vaultViewModel.selectTab(uiState.visibleTabs.first())
            return@LaunchedEffect
        }
        val targetIndex = uiState.visibleTabs.indexOf(uiState.selectedTab)
        if (pagerState.settledPage != targetIndex && pagerState.pageCount > targetIndex) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }

    LaunchedEffect(pagerState, uiState.visibleTabs) {
        snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect { page ->
            val newTab = uiState.visibleTabs.getOrNull(page) ?: return@collect
            if (newTab != uiState.selectedTab) {
                vaultViewModel.selectTab(newTab)
            }
        }
    }

    LaunchedEffect(scrollBehavior.state.collapsedFraction, isStatusBarAutoHide) {
        if (!isStatusBarAutoHide) {
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.show(WindowInsetsCompat.Type.statusBars())
            return@LaunchedEffect
        }
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (scrollBehavior.state.collapsedFraction > 0.6f) {
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        } else if (scrollBehavior.state.collapsedFraction < 0.4f) {
            insetsController.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    val latestTotpStates by rememberUpdatedState(uiState.totpStates)

    val performCopy = remember(
        activity,
        context,
        vaultViewModel,
        mainViewModel,
        decryptAuthTitle,
        decryptAuthSubtitle,
        totpCopiedText,
        fieldCopiedFormat
    ) {
        { fieldKey: FieldKey, item: VaultSummary ->
            val strategy = EntryTypeStrategyFactory.getStrategy(item.entryType)
            val label = strategy.getCopyLabel(fieldKey)

            if (fieldKey == FieldKey.PASSWORD && !item.totpSecret.isNullOrBlank()) {
                latestTotpStates[item.id]?.let { state ->
                    if (state.code.isNotEmpty() && !state.code.contains("-")) {
                        ClipboardUtils.copy(activity, state.code)
                        Toast.makeText(context, totpCopiedText, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                vaultViewModel.loadEntryById(item.id) { fullEntry ->
                    val rawValue =
                        strategy.getFieldValue(fullEntry, fieldKey) ?: return@loadEntryById
                    vaultViewModel.decryptSingle(
                        activity = activity,
                        encryptedData = rawValue,
                        promptTitle = decryptAuthTitle,
                        promptSubtitle = decryptAuthSubtitle,
                        authenticate = { act, t, s, _, ok ->
                            mainViewModel.requestAuth(
                                act, t, s, onSuccess = ok
                            )
                        },
                        onResult = { decrypted ->
                            decrypted?.let {
                                ClipboardUtils.copy(activity, it)
                                Toast.makeText(
                                    context, fieldCopiedFormat.format(label), Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                }
            }
        }
    }

    val onSwipeTriggered = remember(
        activity, mainViewModel, vaultViewModel, authTitle, onShowDetail, performCopy
    ) {
        { action: SwipeActionType, item: VaultSummary ->
            handleSwipeAction(
                actionType = action,
                item = item,
                onAuthRequired = { ok ->
                    mainViewModel.requestAuth(
                        activity, authTitle, item.title, onSuccess = ok
                    )
                },
                onQuickDelete = { vaultViewModel.quickDelete(it) },
                onCopy = { fieldKey -> performCopy(fieldKey, item) },
                onShowDetail = { vaultViewModel.loadEntryById(item.id) { onShowDetail(it) } })
        }
    }

    var pendingManualExportFileName by remember { mutableStateOf<String?>(null) }
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
            it?.let { selectedUri ->
                settingsViewModel.backup.startExport(
                    selectedUri, fileNameHint = pendingManualExportFileName
                )
            }
            pendingManualExportFileName = null
        }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { settingsViewModel.backup.startImport(it) }
    }

    val fabScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1f) isFabVisible = false
                else if (available.y > 1f) isFabVisible = true
                return Offset.Zero
            }
        }
    }

    val onUpdateInteraction = remember(mainViewModel) {
        { mainViewModel.handleIntent(MainIntent.UpdateInteraction) }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onUpdateInteraction
            )
            .then(
                if (isTopBarCollapsible || isTabBarCollapsible || isStatusBarAutoHide) Modifier.nestedScroll(
                    scrollBehavior.nestedScrollConnection
                )
                else Modifier
            )
            .nestedScroll(fabScrollConnection), topBar = {
            androidx.compose.foundation.layout.Column {
                VaultTopBar(
                    vaultViewModel = vaultViewModel,
                    uiState = uiState,
                    scrollBehavior = scrollBehavior,
                    onExportClick = {
                        val started = settingsViewModel.backup.tryStartExportInConfiguredDirectory(
                            settingsUiState.backupDirectoryUri
                        )
                        if (!started) {
                            val manualFileName = settingsViewModel.backup.nextBackupFileName()
                            pendingManualExportFileName = manualFileName
                            exportLauncher.launch(manualFileName)
                        }
                    },
                    onPlainJsonExportClick = onPlainExportClick,
                    onImportClick = {
                        importLauncher.launch(
                            arrayOf(
                                "application/octet-stream", "*/*"
                            )
                        )
                    },
                    onSettingsClick = onSettingsClick,
                    isStatusBarAutoHide = isStatusBarAutoHide,
                    isTopBarCollapsible = isTopBarCollapsible,
                    isTabBarCollapsible = isTabBarCollapsible
                )

                if (uiState.isVaultItemsLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }, floatingActionButton = {
            VaultFab(
                viewModel = vaultViewModel, isVisible = isFabVisible
            )
        }, contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState, modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { pageIndex ->
            val currentTab = uiState.visibleTabs.getOrNull(pageIndex) ?: VaultTab.ALL
            val displayItems = uiState.vaultItemsByTab[currentTab] ?: emptyList()

            if (displayItems.isEmpty() && !uiState.isVaultItemsLoading) {
                EmptyVaultPlaceholder()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = displayItems, key = { it.id }) { item ->
                        VaultListItemRow(
                            item = item,
                            perTypeStyleMap = perTypeStyleMap,
                            defaultCardStyle = settingsUiState.cardStyle,
                            swipeLeftAction = swipeLeftAction,
                            swipeRightAction = swipeRightAction,
                            isSwipeEnabled = isSwipeEnabled,
                            onSwipeTriggered = onSwipeTriggered,
                            vaultViewModel = vaultViewModel
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
            }
        }
    }

    VaultDialogs(
        mainViewModel = mainViewModel,
        activity = activity,
        vaultViewModel = vaultViewModel,
        settingsViewModel = settingsViewModel,
        onUpdateInteraction = onUpdateInteraction
    )
}

@Composable
private fun VaultListItemRow(
    item: VaultSummary,
    perTypeStyleMap: Map<Int, VaultCardStyle>,
    defaultCardStyle: VaultCardStyle,
    swipeLeftAction: SwipeActionType,
    swipeRightAction: SwipeActionType,
    isSwipeEnabled: Boolean,
    onSwipeTriggered: (SwipeActionType, VaultSummary) -> Unit,
    vaultViewModel: VaultViewModel
) {
    val cardStyle = remember(item.entryType, perTypeStyleMap, defaultCardStyle) {
        perTypeStyleMap[item.entryType]?.takeIf { it != VaultCardStyle.DEFAULT }
            ?: VaultCardStyle.resolveForEntryType(defaultCardStyle, item.entryType)
    }
    val colorScheme = MaterialTheme.colorScheme
    val actions =
        remember(item.id, swipeLeftAction, swipeRightAction, onSwipeTriggered, colorScheme) {
            listOfNotNull(
                createSwipeAction(
                    actionType = swipeLeftAction,
                    direction = SwipeDirection.LEFT,
                    onAction = { onSwipeTriggered(swipeLeftAction, item) },
                    backgroundColor = if (swipeLeftAction == SwipeActionType.DELETE) colorScheme.error else colorScheme.primary,
                    iconTint = Color.White
                ), createSwipeAction(
                    actionType = swipeRightAction,
                    direction = SwipeDirection.RIGHT,
                    onAction = { onSwipeTriggered(swipeRightAction, item) },
                    backgroundColor = if (swipeRightAction == SwipeActionType.DELETE) colorScheme.error else colorScheme.secondary,
                    iconTint = Color.White
                )
            )
        }

    SwipeToAction(
        actions = actions,
        modifier = Modifier.fillMaxWidth(),
        isActive = isSwipeEnabled,
    ) {
        VaultCardStyleRegistry.RenderVaultItem(
            style = cardStyle, entry = item, viewModel = vaultViewModel
        )
    }
}