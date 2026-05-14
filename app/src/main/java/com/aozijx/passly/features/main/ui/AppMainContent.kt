package com.aozijx.passly.features.main.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.aozijx.passly.R
import com.aozijx.passly.core.designsystem.components.PlainExportDialog
import com.aozijx.passly.core.designsystem.components.PlainExportDialogType
import com.aozijx.passly.core.di.appViewModelFactory
import com.aozijx.passly.core.navigation.PasslyNavHost
import com.aozijx.passly.features.main.MainViewModel
import com.aozijx.passly.features.settings.SettingsViewModel
import com.aozijx.passly.features.vault.VaultViewModel

@Composable
internal fun AppMainContent(
    activity: FragmentActivity,
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    onPlainExportPickerRequest: (String) -> Unit
) {
    val context = LocalContext.current
    val vaultViewModel: VaultViewModel = viewModel(
        factory = appViewModelFactory(activity.application)
    )
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    var showPlainExportRiskDialog by remember { mutableStateOf(false) }
    val navController = rememberNavController()

    PasslyNavHost(
        navController = navController,
        activity = activity,
        mainViewModel = mainViewModel,
        vaultViewModel = vaultViewModel,
        settingsViewModel = settingsViewModel,
        onPlainExportClick = { showPlainExportRiskDialog = true }
    )

    if (showPlainExportRiskDialog) {
        PlainExportDialog(
            type = PlainExportDialogType.NormalExport,
            onExportBackup = {
                showPlainExportRiskDialog = false
                mainViewModel.requestAuth(
                    activity = activity,
                    title = activity.getString(R.string.vault_backup_auth_title),
                    subtitle = activity.getString(R.string.vault_backup_auth_subtitle_plain_export),
                    onSuccess = {
                        settingsViewModel.backup.issuePlainExportToken()
                        settingsViewModel.backup.exportPlainBackup(
                            context = context,
                            dirUri = settingsUiState.backupDirectoryUri,
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