package com.aozijx.passly.ui.features.vault.internal

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
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.config.UserConfig.Vault.SwipeActionType
import com.aozijx.passly.domain.model.FieldKey
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.domain.strategy.EntryTypeStrategyFactory
import com.aozijx.passly.ui.features.backup.BackupCoordinator
import com.aozijx.passly.ui.features.main.MainViewModel
import com.aozijx.passly.ui.features.main.contract.MainIntent
import com.aozijx.passly.ui.features.vault.VaultViewModel
import com.aozijx.passly.ui.features.vault.contract.VaultUiState

class VaultActionProvider(
    val onSwipeTriggered: (SwipeActionType, VaultSummary) -> Unit,
    val onUpdateInteraction: () -> Unit,
    val fabScrollConnection: NestedScrollConnection,
    val onExportClick: () -> Unit,
    val onImportClick: () -> Unit
)

@Composable
fun rememberVaultActionProvider(
    activity: FragmentActivity,
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel,
    backupCoordinator: BackupCoordinator,
    backupDirectoryUri: String?,
    uiState: VaultUiState,
    onShowDetail: (VaultEntry) -> Unit,
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
        activity, context, vaultViewModel, mainViewModel,
        decryptAuthTitle, decryptAuthSubtitle, totpCopiedText, fieldCopiedFormat
    ) {
        { fieldKey: FieldKey, item: VaultSummary ->
            val strategy = EntryTypeStrategyFactory.getStrategy(item.entryType)
            val label = strategy.getCopyLabel(fieldKey)

            if (fieldKey == FieldKey.PASSWORD && !item.totpSecret.isNullOrBlank()) {
                latestTotpStates[item.id]?.let { state ->
                    if (state.code.isNotEmpty() && !state.code.contains("-")) {
                        ClipboardUtils.copy(activity, state.code)
                        Toast.makeText(context, totpCopiedText, Toast.LENGTH_SHORT).show()
                    }
                } ?: Unit
            } else {
                vaultViewModel.loadEntryById(item.id) { fullEntry ->
                    val rawValue =
                        strategy.getFieldValue(fullEntry, fieldKey) ?: return@loadEntryById
                    vaultViewModel.decryptSingle(
                        activity = activity,
                        encryptedData = rawValue,
                        promptTitle = decryptAuthTitle,
                        promptSubtitle = decryptAuthSubtitle,
                        authenticate = { act, t, s, _, ok ->
                            mainViewModel.requestAuth(act, t, s, onSuccess = ok)
                        },
                        onResult = { decrypted ->
                            decrypted?.let {
                                ClipboardUtils.copy(activity, it)
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
        activity, mainViewModel, vaultViewModel, authTitle, onShowDetail, performCopy
    ) {
        { action: SwipeActionType, item: VaultSummary ->
            handleSwipeAction(
                actionType = action,
                item = item,
                onAuthRequired = { ok ->
                    mainViewModel.requestReauth(
                        activity, authTitle, item.title, onSuccess = ok
                    )
                },
                onQuickDelete = { vaultViewModel.quickDelete(it) },
                onCopy = { fieldKey -> performCopy(fieldKey, item) },
                onShowDetail = { vaultViewModel.loadEntryById(item.id) { onShowDetail(it) } })
        }
    }

    var pendingManualExportFileName by remember { mutableStateOf<String?>(null) }

    val exportLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
            uri?.let { selectedUri ->
                backupCoordinator.startExport(
                    selectedUri, fileNameHint = pendingManualExportFileName
                )
            }
            pendingManualExportFileName = null
        }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { backupCoordinator.startImport(it) }
    }

    val onExportClick = remember(backupCoordinator, exportLauncher) {
        {
            val started = backupCoordinator.tryStartExportInConfiguredDirectory(
                backupDirectoryUri
            )
            if (!started) {
                val manualFileName = backupCoordinator.nextBackupFileName()
                pendingManualExportFileName = manualFileName
                exportLauncher.launch(manualFileName)
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