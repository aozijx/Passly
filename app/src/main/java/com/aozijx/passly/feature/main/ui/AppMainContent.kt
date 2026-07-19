package com.aozijx.passly.feature.main.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.aozijx.passly.feature.backup.BackupViewModel
import com.aozijx.passly.feature.backup.components.PlainExportDialog
import com.aozijx.passly.feature.backup.components.PlainExportDialogType
import com.aozijx.passly.feature.backup.contract.BackupIntent
import com.aozijx.passly.feature.main.MainViewModel
import com.aozijx.passly.feature.settings.datamanagement.DataViewModel
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.ui.navigation.PasslyNavHost

@Composable
internal fun AppMainContent(
    mainViewModel: MainViewModel,
    backupViewModel: BackupViewModel
) {
    val vaultViewModel: VaultViewModel = hiltViewModel()
    val dataViewModel: DataViewModel = hiltViewModel()
    val dataState by dataViewModel.config.collectAsStateWithLifecycle()
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    var showPlainExportRiskDialog by remember { mutableStateOf(false) }
    val navController = rememberNavController()

    PasslyNavHost(
        navController = navController,
        mainViewModel = mainViewModel,
        vaultViewModel = vaultViewModel,
        backupViewModel = backupViewModel,
        onPlainExportClick = { showPlainExportRiskDialog = true },
        isDatabaseInitializing = mainUiState.isDatabaseInitializing
    )

    if (showPlainExportRiskDialog) {
        PlainExportDialog(
            type = PlainExportDialogType.NormalExport,
            onExportBackup = {
                showPlainExportRiskDialog = false
                mainViewModel.requestAuth(
                    onSuccess = {
                        backupViewModel.onIntent(BackupIntent.IssuePlainExportToken)
                        backupViewModel.onIntent(
                            BackupIntent.ExportPlainBackup(
                                dirUri = dataState.directoryUri
                            )
                        )
                    }
                )
            },
            onResetOrCancel = { showPlainExportRiskDialog = false }
        )
    }
}
