package com.aozijx.passly.feature.main.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.aozijx.passly.R
import com.aozijx.passly.feature.backup.BackupCoordinator
import com.aozijx.passly.feature.backup.components.PlainExportDialog
import com.aozijx.passly.feature.backup.components.PlainExportDialogType
import com.aozijx.passly.feature.main.MainViewModel
import com.aozijx.passly.feature.settings.datamanagement.DataViewModel
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.ui.navigation.PasslyNavHost

@Composable
internal fun AppMainContent(
    mainViewModel: MainViewModel,
    backupCoordinator: BackupCoordinator,
    onPlainExportPickerRequest: (String) -> Unit
) {
    val context = LocalContext.current
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
        backupCoordinator = backupCoordinator,
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
                        backupCoordinator.issuePlainExportToken()
                        backupCoordinator.exportPlainBackup(
                            context = context,
                            dirUri = dataState.directoryUri,
                            onPickerRequest = { fileName ->
                                onPlainExportPickerRequest(fileName)
                            }
                        )
                    }
                )
            },
            onResetOrCancel = { showPlainExportRiskDialog = false }
        )
    }
}
