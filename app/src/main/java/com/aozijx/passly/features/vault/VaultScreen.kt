package com.aozijx.passly.features.vault

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aozijx.passly.AppContext
import com.aozijx.passly.MainViewModel
import com.aozijx.passly.R
import com.aozijx.passly.core.common.AddType
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.common.VaultCardStyle
import com.aozijx.passly.core.common.VaultTab
import com.aozijx.passly.core.crypto.CryptoManager
import com.aozijx.passly.core.designsystem.base.VaultItem
import com.aozijx.passly.core.designsystem.components.AutoFillItem
import com.aozijx.passly.core.designsystem.components.TwoFAItem
import com.aozijx.passly.core.designsystem.components.VaultDialogs
import com.aozijx.passly.core.designsystem.components.VaultFab
import com.aozijx.passly.core.designsystem.components.VaultScanner
import com.aozijx.passly.core.designsystem.components.VaultTopBar
import com.aozijx.passly.core.designsystem.components.entries.TypedVaultItemRouter
import com.aozijx.passly.core.designsystem.widgets.EmptyVaultPlaceholder
import com.aozijx.passly.core.designsystem.widgets.SwipeDirection
import com.aozijx.passly.core.designsystem.widgets.SwipeToAction
import com.aozijx.passly.core.designsystem.widgets.createSwipeAction
import com.aozijx.passly.core.designsystem.widgets.handleSwipeAction
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.features.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultContent(
    activity: FragmentActivity,
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    onSettingsClick: () -> Unit = {},
    onShowDetail: (VaultEntry) -> Unit = {}
) {
    val items by vaultViewModel.vaultItems.collectAsState()
    val selectedTab by vaultViewModel.selectedTab.collectAsState()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    val vaultPrefs = remember { AppContext.get().preference }
    val isSwipeEnabled by vaultPrefs.isSwipeEnabled.collectAsState(initial = true)
    val swipeLeftAction by vaultPrefs.swipeLeftAction.collectAsState(initial = SwipeActionType.DELETE)
    val swipeRightAction by vaultPrefs.swipeRightAction.collectAsState(initial = SwipeActionType.DISABLED)
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    
    // 沉浸式设置状态
    val isStatusBarAutoHide = settingsUiState.isStatusBarAutoHide
    val isTopBarCollapsible = settingsUiState.isTopBarCollapsible
    val isTabBarCollapsible = settingsUiState.isTabBarCollapsible
    val cardStyle = settingsUiState.cardStyle
    
    var isFabVisible by remember { mutableStateOf(true) }
    val pagerState = rememberPagerState(initialPage = selectedTab.ordinal) { VaultTab.entries.size }

    // 处理快捷方式进入扫码的逻辑
    LaunchedEffect(Unit) {
        val intent = activity.intent
        if (intent?.getBooleanExtra("START_SCAN", false) == true) {
            vaultViewModel.addType = AddType.SCAN
            // 消费掉这个标志，防止旋转屏幕等操作再次触发
            intent.removeExtra("START_SCAN")
        }
    }

    val fabScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1) isFabVisible = false else if (available.y > 1) isFabVisible = true
                return Offset.Zero
            }
        }
    }

    val categoryAutofill = stringResource(R.string.category_autofill)

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab.ordinal) {
            pagerState.scrollToPage(selectedTab.ordinal)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val newTab = VaultTab.entries[pagerState.currentPage]
        if (newTab != selectedTab) {
            vaultViewModel.selectTab(newTab)
        }
    }

    // 处理状态栏隐藏逻辑
    LaunchedEffect(scrollBehavior.state.collapsedFraction, isStatusBarAutoHide) {
        if (!isStatusBarAutoHide) {
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.show(WindowInsetsCompat.Type.statusBars())
            return@LaunchedEffect
        }
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        // 增加阈值并添加平滑处理，防止频繁切换导致手势冲突
        if (scrollBehavior.state.collapsedFraction > 0.6f) {
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        } else if (scrollBehavior.state.collapsedFraction < 0.4f) {
            insetsController.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { 
        it?.let { settingsViewModel.startExport(it) } 
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { 
        it?.let { settingsViewModel.startImport(it) } 
    }

    LaunchedEffect(settingsViewModel.backupMessage) {
        settingsViewModel.backupMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            settingsViewModel.clearBackupMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            // 核心修复：只有在至少有一个组件需要折叠时，才挂载 scrollBehavior 的 nestedScrollConnection
            modifier = Modifier
                .then(if (isTopBarCollapsible || isTabBarCollapsible || isStatusBarAutoHide) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier)
                .nestedScroll(fabScrollConnection),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                VaultTopBar(
                    vaultViewModel = vaultViewModel,
                    scrollBehavior = scrollBehavior,
                    onExportClick = { exportLauncher.launch("vault_backup_${System.currentTimeMillis()}.poop") },
                    onImportClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                    onSettingsClick = onSettingsClick,
                    isTopBarCollapsible = isTopBarCollapsible,
                    isTabBarCollapsible = isTabBarCollapsible,
                    isStatusBarAutoHide = isStatusBarAutoHide
                )
            },
            floatingActionButton = { 
                VaultFab(
                    viewModel = vaultViewModel,
                    isVisible = isFabVisible
                ) 
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.surface)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val currentTab = VaultTab.entries[pageIndex]
                    val listState = rememberLazyListState()
                    val filteredItems = when (currentTab) {
                        VaultTab.ALL -> items
                        VaultTab.PASSWORDS -> items.filter { it.totpSecret.isNullOrBlank() }
                        VaultTab.TOTP -> items.filter { !it.totpSecret.isNullOrBlank() }
                    }
                    
                    if (filteredItems.isEmpty()) {
                        EmptyVaultPlaceholder()
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(), 
                            contentPadding = PaddingValues(16.dp), 
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredItems, key = { it.id }) { item -> 
                                val itemContent = @Composable {
                                    when {
                                        !item.totpSecret.isNullOrBlank() -> {
                                            TwoFAItem(
                                                entry = item,
                                                vaultViewModel = vaultViewModel,
                                                showCode = vaultViewModel.showTOTPCode
                                            )
                                        }
                                        item.category == categoryAutofill || item.associatedDomain != null || item.associatedAppPackage != null -> {
                                            AutoFillItem(entry = item, viewModel = vaultViewModel)
                                        }
                                        else -> {
                                            if (cardStyle == VaultCardStyle.TYPED) {
                                                TypedVaultItemRouter(entry = item, viewModel = vaultViewModel)
                                            } else {
                                                VaultItem(entry = item, viewModel = vaultViewModel)
                                            }
                                        }
                                    }
                                }
                                
                                if (isSwipeEnabled) {
                                    val leftAction = createSwipeAction(
                                        actionType = swipeLeftAction,
                                        direction = SwipeDirection.LEFT,
                                        onAction = {
                                            handleSwipeAction(
                                                swipeLeftAction, item,
                                                onAuthRequired = { onSuccess -> mainViewModel.authenticate(activity, "确认验证", item.title, null, onSuccess) },
                                                onQuickDelete = { vaultViewModel.quickDelete(it) },
                                                onCopyPassword = { password -> 
                                                    ClipboardUtils.copy(activity, password)
                                                    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                                                },
                                                onDecryptPassword = { callback ->
                                                    try {
                                                        val decrypted = CryptoManager.decrypt(item.password)
                                                        callback(decrypted)
                                                    } catch (e: Exception) {
                                                        callback(null)
                                                    }
                                                },
                                                onShowDetail = {
                                                    vaultViewModel.loadEntryById(item.id) { entry ->
                                                        onShowDetail(entry)
                                                    }
                                                },
                                                onShowEditDetail = {
                                                    vaultViewModel.loadEntryById(item.id) { entry ->
                                                        vaultViewModel.showDetailForEdit(entry)
                                                    }
                                                }
                                            )
                                        },
                                        backgroundColor = MaterialTheme.colorScheme.error,
                                        iconTint = MaterialTheme.colorScheme.onError
                                    )
                                    val rightAction = createSwipeAction(
                                        actionType = swipeRightAction,
                                        direction = SwipeDirection.RIGHT,
                                        onAction = {
                                            handleSwipeAction(
                                                swipeRightAction, item,
                                                onAuthRequired = { onSuccess -> mainViewModel.authenticate(activity, "确认验证", item.title, null, onSuccess) },
                                                onQuickDelete = { vaultViewModel.quickDelete(it) },
                                                onCopyPassword = { password -> 
                                                    ClipboardUtils.copy(activity, password)
                                                    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                                                },
                                                onDecryptPassword = { callback ->
                                                    try {
                                                        val decrypted = CryptoManager.decrypt(item.password)
                                                        callback(decrypted)
                                                    } catch (e: Exception) {
                                                        callback(null)
                                                    }
                                                },
                                                onShowDetail = {
                                                    vaultViewModel.loadEntryById(item.id) { entry ->
                                                        onShowDetail(entry)
                                                    }
                                                },
                                                onShowEditDetail = {
                                                    vaultViewModel.loadEntryById(item.id) { entry ->
                                                        vaultViewModel.showDetailForEdit(entry)
                                                    }
                                                }
                                            )
                                        },
                                        backgroundColor = MaterialTheme.colorScheme.primary,
                                        iconTint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    val actions = listOfNotNull(leftAction, rightAction)
                                    if (actions.isNotEmpty()) {
                                        SwipeToAction(
                                            actions = actions,
                                            isActive = vaultViewModel.itemToDelete?.id != item.id
                                        ) {
                                            itemContent()
                                        }
                                    } else {
                                        itemContent()
                                    }
                                } else {
                                    itemContent()
                                }
                            }
                            item { Spacer(modifier = Modifier.navigationBarsPadding().height(80.dp)) }
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
        }

        AnimatedVisibility(
            visible = vaultViewModel.addType == AddType.SCAN,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                VaultScanner(
                    vaultViewModel = vaultViewModel,
                    onDismiss = { vaultViewModel.addType = AddType.NONE }
                )
            }
        }
    }
}



