package com.aozijx.passly.feature.detail.sections

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.feature.detail.components.TotpCard
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.vault.model.OtpUiState

@Composable
fun TotpSection(
    modifier: Modifier = Modifier,
    currentState: OtpUiState?,
    totpUri: String? = null,
    showProgress: Boolean = true,
    onEvent: (DetailIntent) -> Unit,
) {
    val context = LocalContext.current
    val msgCopySuccess = stringResource(R.string.msg_copy_success)
    val totpLabel = stringResource(R.string.vault_detail_totp_label)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TotpCard(
            currentState = currentState,
            totpUri = totpUri,
            showProgress = showProgress,
            onCodeClick = {
                currentState?.let { state ->
                    val code = state.code
                    if (!code.isNullOrEmpty() && !code.contains("-")) {
                        ClipboardUtils.copy(context, code)
                        Toast.makeText(
                            context,
                            msgCopySuccess.format(totpLabel),
                            Toast.LENGTH_SHORT
                        ).show()
                        onEvent(DetailIntent.RecordAction("totp", ActivityType.COPY_PASSWORD))
                    }
                }
            }
        )
    }
}
