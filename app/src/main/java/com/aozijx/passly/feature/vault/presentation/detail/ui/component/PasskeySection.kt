package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPasskeyDataUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPasskeyUiModel

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
    val (passkeyData, isPasskeyDataRevealed) =
        when (val data = model.data) {
            DetailPasskeyDataUiModel.Absent -> notSet to true
            DetailPasskeyDataUiModel.Hidden -> null to false
            is DetailPasskeyDataUiModel.Revealed -> data.value to true
        }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = passkeyDataLabel,
            value = passkeyData,
            isRevealed = isPasskeyDataRevealed,
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
