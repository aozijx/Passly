package com.example.poop.ui.screens.vault

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.poop.ui.screens.vault.components.AddVaultItemDialog
import com.example.poop.ui.screens.vault.components.BackupPasswordDialog
import com.example.poop.ui.screens.vault.components.DeleteConfirmationDialog
import com.example.poop.ui.screens.vault.components.EmptyVaultPlaceholder
import com.example.poop.ui.screens.vault.components.LoadingMask
import com.example.poop.ui.screens.vault.components.VaultDetailDialog
import com.example.poop.ui.screens.vault.components.VaultItemRow
import com.example.poop.ui.screens.vault.components.VaultTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultContent(activity: FragmentActivity, viewModel: VaultViewModel) {
    val items by viewModel.vaultItems.collectAsState()
    val context = LocalContext.current

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // 状态栏滑动隐藏逻辑
    LaunchedEffect(scrollBehavior.state.collapsedFraction) {
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (scrollBehavior.state.collapsedFraction > 0.5f) {
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    // 文件操作 Launchers
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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            VaultTopAppBar(
                viewModel = viewModel,
                scrollBehavior = scrollBehavior,
                onExportClick = { exportLauncher.launch("vault_backup_${System.currentTimeMillis()}.poop") },
                onImportClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = scrollBehavior.state.collapsedFraction < 0.5f, enter = scaleIn(), exit = scaleOut()) {
                FloatingActionButton(
                    onClick = { viewModel.onAddClick() },
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) { Icon(Icons.Default.Add, contentDescription = "添加") }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.surface)) {
            if (items.isEmpty()) {
                EmptyVaultPlaceholder()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items, key = { it.id }) { item -> VaultItemRow(item = item, viewModel = viewModel) }
                    item { Spacer(modifier = Modifier.navigationBarsPadding().height(80.dp)) }
                }
            }

            // 对话框层
            viewModel.detailItem?.let { item ->
                VaultDetailDialog(activity = activity, item = item, viewModel = viewModel)
            }

            viewModel.itemToDelete?.let { item ->
                DeleteConfirmationDialog(
                    activity = activity,
                    item = item,
                    onConfirm = { viewModel.confirmDelete() },
                    onDismiss = { viewModel.dismissDeleteDialog() }
                )
            }

            if (viewModel.showBackupPasswordDialog) {
                BackupPasswordDialog(activity = activity, viewModel = viewModel)
            }

            if (viewModel.isBackupLoading) {
                LoadingMask(message = if (viewModel.isExporting) "正在导出备份..." else "正在导入恢复...")
            }
        }

        if (viewModel.showAddDialog) {
            AddVaultItemDialog(activity = activity, viewModel = viewModel)
        }
    }
}
