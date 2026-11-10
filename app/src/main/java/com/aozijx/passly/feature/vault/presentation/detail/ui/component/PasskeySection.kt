package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPasskeyUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.ProtectedDetailFieldUiState

@Composable
fun PasskeySection(
    model: DetailPasskeyUiModel,
    onPasskeyCopy: () -> Unit,
    onPasskeyReveal: () -> Unit,
    onHardwareKeyCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val passkeyDataLabel = stringResource(R.string.passkey_data)
    val hardwareKeyInfoLabel = stringResource(R.string.hardware_key_info)
    val notSet = stringResource(R.string.not_set)
    val passkeyData =
        when (val data = model.data) {
            ProtectedDetailFieldUiState.Absent -> notSet
            ProtectedDetailFieldUiState.Hidden -> null
            is ProtectedDetailFieldUiState.Revealed -> data.value
        }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = passkeyDataLabel,
            value = passkeyData,
            isRevealed = model.data !is ProtectedDetailFieldUiState.Hidden,
            onCopy = onPasskeyCopy,
            onEdit = null,
            onReveal = onPasskeyReveal,
        )

        model.hardwareKeyInfo?.let { info ->
            DetailItem(
                label = hardwareKeyInfoLabel,
                value = info,
                isRevealed = true,
                onCopy = onHardwareKeyCopy,
                onEdit = null,
            )
        }
    }
}
