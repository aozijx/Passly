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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import com.example.poop.ui.screens.vault.AddType
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.components.common.EmptyVaultPlaceholder
import com.example.poop.ui.screens.vault.components.common.LoadingMask
import com.example.poop.ui.screens.vault.components.common.VaultFab
import com.example.poop.ui.screens.vault.components.common.VaultTopBar
import com.example.poop.ui.screens.vault.components.dialog.AddPasswordDialog
import com.example.poop.ui.screens.vault.components.dialog.BackupPasswordDialog
import com.example.poop.ui.screens.vault.components.dialog.DeleteConfirmDialog
import com.example.poop.ui.screens.vault.components.dialog.IconPickerDialog
import com.example.poop.ui.screens.vault.components.dialog.PasswordDetailDialog
import com.example.poop.ui.screens.vault.components.items.VaultItemSection
import com.example.poop.ui.screens.vault.twoFA.AddTwoFADialog
import com.example.poop.ui.screens.vault.twoFA.TwoFADetailDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultContent(activity: FragmentActivity, viewModel: VaultViewModel) {
    val items by viewModel.vaultItems.collectAsState()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

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
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                VaultTopBar(
                    viewModel = viewModel,
                    scrollBehavior = scrollBehavior,
                    onExportClick = { exportLauncher.launch("vault_backup_${System.currentTimeMillis()}.poop") },
                    onImportClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) }
                )
            },
            floatingActionButton = { VaultFab(viewModel = viewModel) }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.surface)) {
                if (items.isEmpty()) {
                    EmptyVaultPlaceholder()
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(items, key = { it.id }) { item -> VaultItemSection(entry = item, viewModel = viewModel) }
                        item { Spacer(modifier = Modifier.navigationBarsPadding().height(80.dp)) }
                    }
                }

                viewModel.detailItem?.let { item ->
                    // 统一处理图标选择器
                    if (viewModel.showIconPicker) {
                        IconPickerDialog(
                            onDismiss = { viewModel.showIconPicker = false },
                            currentIconName = item.iconName,
                            currentCustomPath = item.iconCustomPath,
                            onIconSelected = { name -> viewModel.onIconSelected(name) },
                            onCustomImageSelected = { uri -> viewModel.onIconSelected(null, uri.toString()) }
                        )
                    }

                    if (item.totpSecret != null) {
                        // 2FA 详情逻辑已下沉至组件内部，此处直接调用
                        TwoFADetailDialog(
                            activity = activity,
                            item = item,
                            viewModel = viewModel
                        )
                    } else {
                        PasswordDetailDialog(
                            activity = activity,
                            item = item,
                            viewModel = viewModel
                        )
                    }
                }

                viewModel.itemToDelete?.let { item ->
                    DeleteConfirmDialog(activity = activity, item = item, onConfirm = { viewModel.confirmDelete() }, onDismiss = { viewModel.dismissDeleteDialog() })
                }
                if (viewModel.showBackupPasswordDialog) BackupPasswordDialog(activity = activity, viewModel = viewModel)
                if (viewModel.isBackupLoading) LoadingMask(message = if (viewModel.isExporting) "正在导出备份..." else "正在导入恢复...")
            }

            when (viewModel.addType) {
                AddType.PASSWORD -> AddPasswordDialog(activity = activity, viewModel = viewModel)
                AddType.TOTP -> AddTwoFADialog(activity = activity, viewModel = viewModel)
                else -> {}
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
