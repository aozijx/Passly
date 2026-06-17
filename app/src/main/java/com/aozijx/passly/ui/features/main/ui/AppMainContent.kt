package com.aozijx.passly.ui.features.main.ui

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
import com.aozijx.passly.core.di.appViewModelFactory
import com.aozijx.passly.domain.config.UserConfigProvider
import com.aozijx.passly.ui.features.backup.components.PlainExportDialog
import com.aozijx.passly.ui.features.backup.components.PlainExportDialogType
import com.aozijx.passly.ui.features.main.MainViewModel
import com.aozijx.passly.ui.features.vault.VaultViewModel
import com.aozijx.passly.ui.navigation.PasslyNavHost

@Composable
internal fun AppMainContent(
    activity: FragmentActivity,
    mainViewModel: MainViewModel,
    onPlainExportPickerRequest: (String) -> Unit
) {
    val context = LocalContext.current
    val vaultViewModel: VaultViewModel = viewModel(
        factory = appViewModelFactory(activity.application)
    )
    val configProvider: UserConfigProvider = viewModel(
        factory = appViewModelFactory(activity.application)
    )
    val userConfig by configProvider.config.collectAsStateWithLifecycle()
    var showPlainExportRiskDialog by remember { mutableStateOf(false) }
    val navController = rememberNavController()

    PasslyNavHost(
        navController = navController,
        activity = activity,
        mainViewModel = mainViewModel,
        vaultViewModel = vaultViewModel,
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
                        configProvider.backup.issuePlainExportToken()
                        configProvider.backup.exportPlainBackup(
                            context = context,
                            dirUri = userConfig.backup.directoryUri,
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