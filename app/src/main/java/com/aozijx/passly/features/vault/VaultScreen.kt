package com.aozijx.passly.features.vault

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aozijx.passly.AppContext
import com.aozijx.passly.MainViewModel
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.designsystem.model.AddType
import com.aozijx.passly.core.designsystem.model.VaultCardStyle
import com.aozijx.passly.core.designsystem.model.VaultTab
import com.aozijx.passly.core.designsystem.widgets.EmptyVaultPlaceholder
import com.aozijx.passly.core.designsystem.widgets.SwipeDirection
import com.aozijx.passly.core.designsystem.widgets.SwipeToAction
import com.aozijx.passly.core.designsystem.widgets.createSwipeAction
import com.aozijx.passly.core.designsystem.widgets.handleSwipeAction
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.domain.strategy.EntryTypeStrategyFactory
import com.aozijx.passly.features.scanner.VaultScanner
import com.aozijx.passly.features.settings.SettingsViewModel
import com.aozijx.passly.features.vault.components.VaultDialogs
import com.aozijx.passly.features.vault.components.entries.VaultCardStyleRegistry
import com.aozijx.passly.features.vault.components.fab.VaultFab
import com.aozijx.passly.features.vault.components.topbar.VaultTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultContent(
    activity: FragmentActivity,
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    onSettingsClick: () -> Unit = {},
    onPlainExportClick: () -> Unit = {},
    onShowDetail: (VaultEntry) -> Unit = {}
) {
    // 1. 优化：感知生命周期的状态订阅 (Lifecycle-aware)
    val items by vaultViewModel.vaultItems.collectAsStateWithLifecycle()
    val isVaultItemsLoading by vaultViewModel.isVaultItemsLoading.collectAsStateWithLifecycle()
    val selectedTab by vaultViewModel.selectedTab.collectAsStateWithLifecycle()
    val totpStates by vaultViewModel.totpStates.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val vaultPrefs = remember { AppContext.get().preference }

    // 修复：collectAsStateWithLifecycle 不接受 initial 参数，应在 Flow 定义处指定
    val isSwipeEnabled by vaultPrefs.isSwipeEnabled.collectAsStateWithLifecycle(initialValue = true)
    val swipeLeftAction by vaultPrefs.swipeLeftAction.collectAsStateWithLifecycle(initialValue = SwipeActionType.DELETE)
    val swipeRightAction by vaultPrefs.swipeRightAction.collectAsStateWithLifecycle(initialValue = SwipeActionType.DISABLED)

    val pagerState = rememberPagerState(initialPage = selectedTab.ordinal) { VaultTab.entries.size }
    var isFabVisible by remember { mutableStateOf(true) }

    // 1. 同步 ViewModel 状态到 Pager (解决点击切换)
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab.ordinal) {
            pagerState.animateScrollToPage(selectedTab.ordinal)
        }
    }

    // 2. 同步 Pager 滑动到 ViewModel (解决左右滑动)
    LaunchedEffect(pagerState.currentPage) {
        val newTab = VaultTab.entries[pagerState.currentPage]
        if (newTab != selectedTab) {
            vaultViewModel.selectTab(newTab)
        }
    }

    // 通用复制逻辑
    val performCopy: (String, VaultSummary) -> Unit = { field, item ->
        val strategy = EntryTypeStrategyFactory.getStrategy(item.entryType)
        val label = strategy.getCopyLabel(field)

        if (field == "password" && !item.totpSecret.isNullOrBlank()) {
            totpStates[item.id]?.let { state ->
                if (state.code.isNotEmpty() && !state.code.contains("-")) {
                    ClipboardUtils.copy(activity, state.code)
                    Toast.makeText(context, "验证码已复制", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // 通用字段复制逻辑（涉及解密的自动处理）
            val isSensitive = field == "password"
            if (isSensitive) {
                vaultViewModel.loadEntryById(item.id) { fullEntry ->
                    vaultViewModel.decryptSingle(
                        activity = activity,
                        encryptedData = fullEntry.password,
                        authenticate = { act, t, s, _, ok -> mainViewModel.authenticate(act, t, s, null, ok) },
                        onResult = { decrypted ->
                            decrypted?.let {
                                ClipboardUtils.copy(activity, it)
                                Toast.makeText(context, "${label}已复制", Toast.LENGTH_SHORT).show()
                            }
                        })
                }
            } else {
                ClipboardUtils.copy(activity, item.username)
                Toast.makeText(context, "${label}已复制", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 优化滑动逻辑：将左右滑动合并为一个执行闭包
    val onSwipeTriggered: (SwipeActionType, VaultSummary) -> Unit = { action, item ->
        handleSwipeAction(
            actionType = action,
            item = item,
            onAuthRequired = { ok -> mainViewModel.authenticate(activity, "安全验证", item.title, null, ok) },
            onQuickDelete = { vaultViewModel.quickDelete(it) },
            onCopy = { field -> performCopy(field, item) },
            onShowDetail = { vaultViewModel.loadEntryById(item.id) { onShowDetail(it) } },
            onShowEditDetail = { vaultViewModel.loadEntryById(item.id) { vaultViewModel.showDetailForEdit(it) } }
        )
    }

    var pendingManualExportFileName by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
        it?.let { selectedUri -> settingsViewModel.startExport(selectedUri, fileNameHint = pendingManualExportFileName) }
        pendingManualExportFileName = null
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { settingsViewModel.startImport(it) }
    }

    // 实现 FAB 的滑动隐藏显示逻辑
    val fabScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // y < -1 表示向上滑动，隐藏 FAB；y > 1 表示向下滑动，显示 FAB
                if (available.y < -1f) isFabVisible = false
                else if (available.y > 1f) isFabVisible = true
                return Offset.Zero
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .nestedScroll(fabScrollConnection), // 必须挂载 Connection
        topBar = {
            VaultTopBar(
                vaultViewModel = vaultViewModel,
                scrollBehavior = scrollBehavior,
                onExportClick = {
                    val started = settingsViewModel.tryStartExportInConfiguredDirectory(settingsUiState.backupDirectoryUri)
                    if (!started) {
                        val manualFileName = settingsViewModel.nextBackupFileName()
                        pendingManualExportFileName = manualFileName
                        exportLauncher.launch(manualFileName)
                    }
                },
                onPlainJsonExportClick = onPlainExportClick,
                onImportClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                onSettingsClick = onSettingsClick,
                isStatusBarAutoHide = settingsUiState.isStatusBarAutoHide,
                isTopBarCollapsible = settingsUiState.isTopBarCollapsible,
                isTabBarCollapsible = settingsUiState.isTabBarCollapsible
            )
        },
        floatingActionButton = { 
            VaultFab(viewModel = vaultViewModel, isVisible = isFabVisible) 
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) { _ ->
            val displayItems = items

            if (isVaultItemsLoading) {
                VaultListSkeleton()
            } else if (displayItems.isEmpty()) {
                EmptyVaultPlaceholder()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayItems, key = { it.id }) { item ->
                        val itemContent = @Composable {
                            VaultCardStyleRegistry.RenderVaultItem(
                                style = VaultCardStyle.resolveForEntryType(settingsUiState.cardStyleByEntryType[item.entryType] ?: VaultCardStyle.DEFAULT, item.entryType),
                                entry = item,
                                viewModel = vaultViewModel
                            )
                        }

                        if (isSwipeEnabled) {
                            val leftColors = swipeRevealColors(swipeLeftAction)
                            val rightColors = swipeRevealColors(swipeRightAction)

                            SwipeToAction(
                                actions = listOfNotNull(
                                    createSwipeAction(swipeLeftAction, SwipeDirection.LEFT, { onSwipeTriggered(swipeLeftAction, item) }, leftColors.first, leftColors.second),
                                    createSwipeAction(swipeRightAction, SwipeDirection.RIGHT, { onSwipeTriggered(swipeRightAction, item) }, rightColors.first, rightColors.second)
                                ),
                                isActive = vaultViewModel.itemToDelete?.id != item.id
                            ) {
                                itemContent()
                            }
                        } else {
                            itemContent()
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.navigationBarsPadding().height(80.dp))
                    }
                }
            }
        }

        VaultDialogs(
            activity = activity,
            mainViewModel = mainViewModel,
            vaultViewModel = vaultViewModel,
            settingsViewModel = settingsViewModel
        )
    }

    AnimatedVisibility(
        visible = vaultViewModel.addType == AddType.SCAN,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            VaultScanner(
                vaultViewModel = vaultViewModel,
                onDismiss = { vaultViewModel.addType = AddType.NONE })
        }
    }
}

@Composable
private fun swipeRevealColors(actionType: SwipeActionType): Pair<Color, Color> {
    return when (actionType) {
        SwipeActionType.DELETE -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        SwipeActionType.COPY_PASSWORD, SwipeActionType.COPY_USERNAME -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        SwipeActionType.EDIT -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        SwipeActionType.DETAIL -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        SwipeActionType.DISABLED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun VaultListSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(6) {
            Card(
                modifier = Modifier.fillMaxWidth().height(72.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {}
        }
        item {
            Spacer(modifier = Modifier.navigationBarsPadding().height(80.dp))
        }
    }
}
