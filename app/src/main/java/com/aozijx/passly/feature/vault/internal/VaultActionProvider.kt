package com.aozijx.passly.feature.vault.internal

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.feature.main.MainViewModel
import com.aozijx.passly.feature.main.contract.MainIntent
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.feature.vault.model.OtpUiState
import com.aozijx.passly.feature.vault.strategy.EntryTypeDisplayProvider

class VaultActionProvider(
    val onSwipeTriggered: (SwipeActionType, EntryListItem) -> Unit,
    val onUpdateInteraction: () -> Unit,
    val fabScrollConnection: NestedScrollConnection
)

@Composable
fun rememberVaultActionProvider(
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel,
    totpStates: Map<String, OtpUiState>,
    onShowDetail: (EntryListItem) -> Unit,
    isFabVisible: (Boolean) -> Unit
): VaultActionProvider {
    val context = LocalContext.current
    val decryptAuthTitle = stringResource(R.string.auth_title)
    val decryptAuthSubtitle = stringResource(R.string.vault_auth_decrypt_subtitle_generic)
    val totpCopiedText = stringResource(R.string.vault_totp_copied)
    val fieldCopiedFormat = stringResource(R.string.vault_field_copied_format)
    val authTitle = stringResource(R.string.auth_title)

    val latestTotpStates by rememberUpdatedState(totpStates)

    val performCopy = remember(
        context, vaultViewModel, mainViewModel,
        decryptAuthTitle, decryptAuthSubtitle, totpCopiedText, fieldCopiedFormat
    ) {
        { fieldKey: FieldKey, item: EntryListItem ->
            val label = EntryTypeDisplayProvider.getCopyLabel(fieldKey)

            if (fieldKey == FieldKey.PASSWORD && item.hasOtp) {
                latestTotpStates[item.id]?.let { state ->
                    val code = state.code
                    if (!code.isNullOrEmpty() && !code.contains("-")) {
                        ClipboardUtils.copy(context, code)
                        Toast.makeText(context, totpCopiedText, Toast.LENGTH_SHORT).show()
                    }
                } ?: Unit
            } else {
                vaultViewModel.loadEntryById(item.id) { fullEntry ->
                    val rawValue =
                        vaultViewModel.entryFieldReader.getFieldValue(fullEntry, fieldKey)
                            ?: return@loadEntryById
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
        { action: SwipeActionType, item: EntryListItem ->
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
                onShowDetail = onShowDetail
            )
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
        fabScrollConnection = fabScrollConnection
    )
}
