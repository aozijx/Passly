package com.aozijx.passly.feature.vault.internal

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.entry.FieldKey
import com.aozijx.passly.domain.model.lookup.VaultListItem
import com.aozijx.passly.domain.model.settings.SwipeActionType
import com.aozijx.passly.domain.strategy.EntryTypeStrategyFactory
import com.aozijx.passly.feature.backup.BackupViewModel
import com.aozijx.passly.feature.backup.contract.BackupIntent
import com.aozijx.passly.feature.main.MainViewModel
import com.aozijx.passly.feature.main.contract.MainIntent
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.feature.vault.contract.VaultUiState

class VaultActionProvider(
    val onSwipeTriggered: (SwipeActionType, VaultListItem) -> Unit,
    val onUpdateInteraction: () -> Unit,
    val fabScrollConnection: NestedScrollConnection,
    val onExportClick: () -> Unit,
    val onImportClick: () -> Unit
)

@Composable
fun rememberVaultActionProvider(
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel,
    backupViewModel: BackupViewModel,
    backupDirectoryUri: String?,
    uiState: VaultUiState,
    onShowDetail: (VaultListItem) -> Unit,
    isFabVisible: (Boolean) -> Unit
): VaultActionProvider {
    val context = LocalContext.current
    val decryptAuthTitle = stringResource(R.string.vault_auth_decrypt_title)
    val decryptAuthSubtitle = stringResource(R.string.vault_auth_decrypt_subtitle_generic)
    val totpCopiedText = stringResource(R.string.vault_totp_copied)
    val fieldCopiedFormat = stringResource(R.string.vault_field_copied_format)
    val authTitle = stringResource(R.string.auth_title)

    val latestTotpStates by rememberUpdatedState(uiState.totpStates)

    val performCopy = remember(
        context, vaultViewModel, mainViewModel,
        decryptAuthTitle, decryptAuthSubtitle, totpCopiedText, fieldCopiedFormat
    ) {
        { fieldKey: FieldKey, item: VaultListItem ->
            val strategy = EntryTypeStrategyFactory.getStrategy(item.entryType)
            val label = strategy.getCopyLabel(fieldKey)

            if (fieldKey == FieldKey.PASSWORD && item.hasTotp) {
                latestTotpStates[item.id]?.let { state ->
                    if (state.code.isNotEmpty() && !state.code.contains("-")) {
                        ClipboardUtils.copy(context, state.code)
                        Toast.makeText(context, totpCopiedText, Toast.LENGTH_SHORT).show()
                    }
                } ?: Unit
            } else {
                vaultViewModel.loadEntryById(item.id) { fullEntry ->
                    val rawValue =
                        strategy.getFieldValue(fullEntry, fieldKey) ?: return@loadEntryById
                    vaultViewModel.decryptSingle(
                        encryptedData = rawValue,
                        authenticate = { ok ->
                            mainViewModel.requestAuth(onSuccess = ok)
                        },
                        onResult = { decrypted ->
                            decrypted?.let {
                                ClipboardUtils.copy(context, it)
                                Toast.makeText(
                                    context, fieldCopiedFormat.format(label), Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                }
            }
        }
    }

    val onSwipeTriggered = remember(
        mainViewModel, vaultViewModel, authTitle, onShowDetail, performCopy
    ) {
        { action: SwipeActionType, item: VaultListItem ->
            handleSwipeAction(
                actionType = action,
                item = item,
                onAuthRequired = { ok ->
                    mainViewModel.requestReauth(
                        onSuccess = ok
                    )
                },
                onQuickDelete = { vaultViewModel.quickDelete(it) },
                onCopy = { fieldKey -> performCopy(fieldKey, item) },
                onShowDetail = { vaultViewModel.showDetail(item) })
        }
    }

    var pendingManualExportFileName by remember { mutableStateOf<String?>(null) }

    val exportLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
            uri?.let { selectedUri ->
                backupViewModel.onIntent(
                    BackupIntent.StartExport(
                        uri = selectedUri,
                        fileNameHint = pendingManualExportFileName
                    )
                )
            }
            pendingManualExportFileName = null
        }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { backupViewModel.onIntent(BackupIntent.StartImport(it)) }
    }

    val onExportClick = remember(backupViewModel, exportLauncher, backupDirectoryUri) {
        {
            if (backupDirectoryUri.isNullOrBlank()) {
                val manualFileName = backupViewModel.buildBackupFileName()
                pendingManualExportFileName = manualFileName
                exportLauncher.launch(manualFileName)
            } else {
                backupViewModel.onIntent(
                    BackupIntent.TryStartExportInConfiguredDirectory(
                        backupDirectoryUri
                    )
                )
            }
        }
    }

    val onImportClick = remember(importLauncher) {
        {
            importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
        }
    }

    val onUpdateInteraction = remember(mainViewModel) {
        { mainViewModel.handleIntent(MainIntent.UpdateInteraction) }
    }

    val fabScrollConnection = remember(isFabVisible) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1f) isFabVisible(false)
                else if (available.y > 1f) isFabVisible(true)
                return Offset.Zero
            }
        }
    }

    return VaultActionProvider(
        onSwipeTriggered = onSwipeTriggered,
        onUpdateInteraction = onUpdateInteraction,
        fabScrollConnection = fabScrollConnection,
        onExportClick = onExportClick,
        onImportClick = onImportClick
    )
}
