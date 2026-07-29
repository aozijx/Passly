package com.aozijx.passly.feature.vault.action

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
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.feature.vault.model.OtpUiState
import kotlinx.coroutines.flow.StateFlow

class VaultActionProvider(
    val onSwipeTriggered: (SwipeActionType, EntryListItem) -> Unit,
    val onUpdateInteraction: () -> Unit,
    val fabScrollConnection: NestedScrollConnection
)

@Composable
fun rememberVaultActionProvider(
    vaultViewModel: VaultViewModel,
    totpStates: StateFlow<Map<String, OtpUiState>>,
    requestAuthentication: (onSuccess: () -> Unit) -> Unit,
    requestReauthentication: (onSuccess: () -> Unit) -> Unit,
    requestSensitiveCopy: (onSuccess: () -> Unit) -> Unit,
    onUserInteraction: () -> Unit,
    onShowDetail: (EntryListItem) -> Unit,
    isFabVisible: (Boolean) -> Unit
): VaultActionProvider {
    val context = LocalContext.current
    val totpLabel = stringResource(R.string.vault_detail_totp_label)
    val fieldCopiedFormat = stringResource(R.string.msg_copy_success)

    val latestAuthentication by rememberUpdatedState(requestAuthentication)
    val latestReauthentication by rememberUpdatedState(requestReauthentication)
    val latestSensitiveCopy by rememberUpdatedState(requestSensitiveCopy)
    val latestUserInteraction by rememberUpdatedState(onUserInteraction)

    val performCopy = remember(
        context, vaultViewModel, totpLabel, fieldCopiedFormat
    ) {
        { fieldKey: FieldKey, item: EntryListItem ->
            val label = CopyFieldLabelProvider.getCopyLabel(fieldKey)

            if (fieldKey == FieldKey.PASSWORD && item.hasOtp) {
                totpStates.value[item.id]?.let { state ->
                    val code = state.code
                    if (!code.isNullOrEmpty() && !code.contains("-")) {
                        ClipboardUtils.copy(context, code)
                        Toast.makeText(
                            context, fieldCopiedFormat.format(totpLabel), Toast.LENGTH_SHORT
                        ).show()
                    }
                } ?: Unit
            } else {
                latestAuthentication {
                    vaultViewModel.loadEntryById(item.id) { fullEntry ->
                        val rawValue =
                            vaultViewModel.entryFieldReader.getFieldValue(fullEntry, fieldKey)
                                ?: return@loadEntryById
                        ClipboardUtils.copy(context, rawValue)
                        Toast.makeText(
                            context,
                            fieldCopiedFormat.format(label),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    val onSwipeTriggered = remember(
        vaultViewModel, onShowDetail, performCopy
    ) {
        { action: SwipeActionType, item: EntryListItem ->
            handleSwipeAction(
                actionType = action,
                item = item,
                onDeleteAuthRequired = { ok ->
                    latestReauthentication(ok)
                },
                onCopyAuthRequired = { ok -> latestSensitiveCopy(ok) },
                onQuickDelete = { vaultViewModel.quickDelete(it) },
                onCopy = { fieldKey -> performCopy(fieldKey, item) },
                onShowDetail = onShowDetail
            )
        }
    }

    val onUpdateInteraction = remember { { latestUserInteraction() } }

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
