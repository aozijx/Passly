package com.aozijx.passly.features.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aozijx.passly.core.designsystem.model.TotpState

@Composable
fun TotpCodeCard(
    currentState: TotpState?,
    isSteam: Boolean,
    onQrClick: (() -> Unit)? = null,
    onCodeClick: (() -> Unit)? = null,
    title: String = "两步验证码"
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            if (onQrClick != null) {
                IconButton(onClick = onQrClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.QrCode,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onCodeClick != null) Modifier.clickable(onClick = onCodeClick) else Modifier),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                    alpha = 0.2f
                )
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayText =
                    if (isSteam) (currentState?.code ?: "------") else (currentState?.code?.chunked(
                        3
                    )?.joinToString(" ") ?: "------")
                Text(
                    text = displayText, style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = if (isSteam) 4.sp else 2.sp,
                        fontFamily = FontFamily.Monospace
                    ), color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(24.dp))
                CircularProgressIndicator(
                    progress = { currentState?.progress ?: 0f },
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 4.dp,
                    color = if ((currentState?.progress
                            ?: 1f) < 0.2f
                    ) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}
