package com.aozijx.passly.presentation.feature.vault.detail.section

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
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.component.TotpCard

@Composable
fun TotpSection(
    modifier: Modifier = Modifier,
    currentState: OtpCodeState?,
    totpUri: String? = null,
    showProgress: Boolean = true,
    onAction: (DetailUiAction) -> Unit,
) {
    val context = LocalContext.current
    val msgCopySuccess = stringResource(R.string.field_copy_success_message)
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
                        onAction(DetailUiAction.RecordAction("totp", ActivityType.COPY_PASSWORD))
                    }
                }
            }
        )
    }
}
