package com.aozijx.passly.presentation.ui.vault.detail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R

@Composable
fun PasskeySection(
    hasPasskeyData: Boolean,
    revealedPasskeyData: String?,
    hardwareKeyInfo: String?,
    onPasskeyCopy: () -> Unit,
    onPasskeyReveal: () -> Unit,
    onHardwareKeyCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val passkeyDataLabel = stringResource(R.string.passkey_data)
    val hardwareKeyInfoLabel = stringResource(R.string.hardware_key_info)
    val notSet = stringResource(R.string.not_set)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = passkeyDataLabel,
            value = if (hasPasskeyData) revealedPasskeyData else notSet,
            isRevealed = revealedPasskeyData != null || !hasPasskeyData,
            onCopy = onPasskeyCopy,
            onEdit = null,
            onReveal = onPasskeyReveal,
        )

        if (!hardwareKeyInfo.isNullOrBlank()) {
            DetailItem(
                label = hardwareKeyInfoLabel,
                value = hardwareKeyInfo,
                isRevealed = true,
                onCopy = onHardwareKeyCopy,
                onEdit = null,
            )
        }
    }
}
