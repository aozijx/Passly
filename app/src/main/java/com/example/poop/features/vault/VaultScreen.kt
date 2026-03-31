package com.example.poop.features.vault

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.poop.AppContext
import com.example.poop.MainViewModel
import com.example.poop.R
import com.example.poop.core.common.AddType
import com.example.poop.core.common.SwipeActionType
import com.example.poop.core.common.VaultTab
import com.example.poop.core.crypto.CryptoManager
import com.example.poop.core.designsystem.base.VaultItem
import com.example.poop.core.designsystem.components.AutoFillItem
import com.example.poop.core.designsystem.components.TwoFAItem
import com.example.poop.core.designsystem.components.VaultDialogs
import com.example.poop.core.designsystem.components.VaultFab
import com.example.poop.core.designsystem.components.VaultScanner
import com.example.poop.core.designsystem.components.VaultTopBar
import com.example.poop.core.designsystem.widgets.EmptyVaultPlaceholder
import com.example.poop.core.designsystem.widgets.SwipeDirection
import com.example.poop.core.designsystem.widgets.SwipeToAction
import com.example.poop.core.designsystem.widgets.createSwipeAction
import com.example.poop.core.designsystem.widgets.handleSwipeAction
import com.example.poop.core.util.ClipboardUtils
import com.example.poop.data.model.VaultEntry
import com.example.poop.features.settings.SettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow

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
    
    var isFabVisible by remember { mutableStateOf(true) }
    val pagerState = rememberPagerState(initialPage = selectedTab.ordinal) { VaultTab.entries.size }

    val nestedScrollConnection = remember {
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
            (vaultViewModel.selectedTab as MutableStateFlow).value = newTab
        }
    }

    LaunchedEffect(scrollBehavior.state.collapsedFraction) {
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (scrollBehavior.state.collapsedFraction > 0.5f) {
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        } else {
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
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection).nestedScroll(nestedScrollConnection),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                VaultTopBar(
                    vaultViewModel = vaultViewModel,
                    scrollBehavior = scrollBehavior,
                    onExportClick = { exportLauncher.launch("vault_backup_${System.currentTimeMillis()}.poop") },
                    onImportClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                    onSettingsClick = onSettingsClick
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
                        VaultTab.PASSWORDS -> items.filter { it.totpSecret == null }
                        VaultTab.TOTP -> items.filter { it.totpSecret != null }
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
                                        item.totpSecret != null -> {
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
                                            VaultItem(entry = item, viewModel = vaultViewModel)
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
                                                onAuthRequired = { onSuccess -> mainViewModel.authenticate(activity, "删除确认", item.title, null, onSuccess) },
                                                onQuickDelete = { vaultViewModel.quickDelete(it) },
                                                onCopyPassword = { password -> 
                                                    ClipboardUtils.copy(activity, password)
                                                    Toast.makeText(context, "密码已复制", Toast.LENGTH_SHORT).show()
                                                },
                                                onDecryptPassword = { callback ->
                                                    try {
                                                        val decrypted = CryptoManager.decrypt(item.password)
                                                        callback(decrypted)
                                                    } catch (e: Exception) {
                                                        callback(null)
                                                    }
                                                },
                                                onShowDetail = { onShowDetail(it) }
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
                                                onAuthRequired = { onSuccess -> mainViewModel.authenticate(activity, "删除确认", item.title, null, onSuccess) },
                                                onQuickDelete = { vaultViewModel.quickDelete(it) },
                                                onCopyPassword = { password -> 
                                                    ClipboardUtils.copy(activity, password)
                                                    Toast.makeText(context, "密码已复制", Toast.LENGTH_SHORT).show()
                                                },
                                                onDecryptPassword = { callback ->
                                                    try {
                                                        val decrypted = CryptoManager.decrypt(item.password)
                                                        callback(decrypted)
                                                    } catch (e: Exception) {
                                                        callback(null)
                                                    }
                                                },
                                                onShowDetail = { onShowDetail(it) }
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
