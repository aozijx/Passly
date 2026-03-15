package com.example.poop.ui.screens.vault.components.layout

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
import com.example.poop.R
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.common.EmptyVaultPlaceholder
import com.example.poop.ui.screens.vault.common.base.VaultItem
import com.example.poop.ui.screens.vault.components.items.AutoFillItem
import com.example.poop.ui.screens.vault.components.items.TwoFAItem
import com.example.poop.ui.screens.vault.core.AddType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultContent(activity: FragmentActivity, viewModel: VaultViewModel) {
    val items by viewModel.vaultItems.collectAsState()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    val listState = rememberLazyListState()
    var isFabVisible by remember { mutableStateOf(true) }

    // 使用 NestedScrollConnection 监听滚动，这比直接监听 LazyListState 更稳定
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // available.y < 0 代表上划（内容上移），available.y > 0 代表下划
                if (available.y < -1) {
                    isFabVisible = false
                } else if (available.y > 1) {
                    isFabVisible = true
                }
                return Offset.Zero
            }
        }
    }

    // 预解析字符串资源
    val categoryAutofill = stringResource(R.string.category_autofill)

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
        it?.let { viewModel.startExport(it) } 
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { 
        it?.let { viewModel.startImport(it) } 
    }

    LaunchedEffect(viewModel.backupMessage) {
        viewModel.backupMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearBackupMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            // 合并多个 NestedScrollConnection
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection).nestedScroll(nestedScrollConnection),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                VaultTopBar(
                    viewModel = viewModel,
                    scrollBehavior = scrollBehavior,
                    onExportClick = { exportLauncher.launch("vault_backup_${System.currentTimeMillis()}.poop") },
                    onImportClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) }
                )
            },
            floatingActionButton = { 
                VaultFab(
                    viewModel = viewModel,
                    isVisible = isFabVisible
                ) 
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.surface)) {
                if (items.isEmpty()) {
                    EmptyVaultPlaceholder()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(), 
                        contentPadding = PaddingValues(16.dp), 
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items, key = { it.id }) { item -> 
                            when {
                                item.totpSecret != null -> {
                                    TwoFAItem(entry = item, viewModel = viewModel, showCode = viewModel.showTOTPCode)
                                }
                                item.category == categoryAutofill || item.associatedDomain != null || item.associatedAppPackage != null -> {
                                    AutoFillItem(entry = item, viewModel = viewModel)
                                }
                                else -> {
                                    VaultItem(entry = item, viewModel = viewModel)
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.navigationBarsPadding().height(80.dp)) }
                    }
                }

                VaultDialogs(activity = activity, viewModel = viewModel)
            }
        }

        AnimatedVisibility(
            visible = viewModel.addType == AddType.SCAN,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                VaultScanner(
                    activity = activity,
                    vaultViewModel = viewModel,
                    onDismiss = { viewModel.dismissAddDialog() }
                )
            }
        }
    }
}
