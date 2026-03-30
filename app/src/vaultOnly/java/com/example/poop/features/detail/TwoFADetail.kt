package com.example.poop.features.detail

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.poop.MainViewModel
import com.example.poop.R
import com.example.poop.core.designsystem.DetailActions
import com.example.poop.core.designsystem.DetailHeader
import com.example.poop.core.designsystem.sections.CategoryItem
import com.example.poop.core.designsystem.sections.TotpConfigForm
import com.example.poop.core.designsystem.state.VaultEditState
import com.example.poop.core.crypto.CryptoManager
import com.example.poop.data.model.VaultEntry
import com.example.poop.features.vault.VaultViewModel
import com.example.poop.util.ClipboardUtils
import com.example.poop.core.util.QrCodeUtils
import com.example.poop.features.vault.TotpEditState
import java.net.URLEncoder

@Composable
fun TwoFADetailDialog(
    activity: FragmentActivity,
    item: VaultEntry,
    vaultViewModel: VaultViewModel,
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current
    
    val totpStates by vaultViewModel.totpStates.collectAsState()
    val currentState = totpStates[item.id]
    
    val isSteam = remember(item.totpAlgorithm) { item.totpAlgorithm.uppercase() == "STEAM" }
    var showQrDialog by remember { mutableStateOf(false) }

    LaunchedEffect(item.id) {
        vaultViewModel.autoUnlockTotp(item)
    }

    val authRevealTitle = stringResource(R.string.vault_auth_decrypt_title)
    val authRevealSubtitle = stringResource(R.string.vault_auth_reveal_subtitle)
    val authQrTitle = stringResource(R.string.vault_auth_qr_title)
    val authQrSubtitle = stringResource(R.string.vault_auth_qr_subtitle)
    val totpCopiedMsg = stringResource(R.string.vault_totp_copied)
    
    val categoryEditState = remember(item) { VaultEditState(item) }
    val totpEditState = remember(item, currentState?.decryptedSecret) {
        TotpEditState(item, currentState?.decryptedSecret ?: "")
    }

    AlertDialog(
        onDismissRequest = { vaultViewModel.dismissDetail() },
        title = {
            DetailHeader(
                item = item,
                onIconClick = { vaultViewModel.showIconPicker = true },
                onMoreClick = {
                    if (totpEditState.isEditing) {
                        totpEditState.isEditing = false
                    } else {
                        mainViewModel.authenticate(
                            activity = activity,
                            title = authRevealTitle,
                            subtitle = authRevealSubtitle,
                            onSuccess = { 
                                totpEditState.secret = currentState?.decryptedSecret ?: ""
                                totpEditState.isEditing = true 
                            }
                        )
                    }
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                CategoryItem(viewModel = vaultViewModel, entry = item, editState = categoryEditState)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.vault_totp_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    IconButton(
                        onClick = {
                            mainViewModel.authenticate(
                                activity = activity,
                                title = authQrTitle,
                                subtitle = authQrSubtitle,
                                onSuccess = {
                                    totpEditState.isEditing = false
                                    showQrDialog = true
                                } 
                            )
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.QrCode, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (currentState != null && currentState.code.isNotEmpty() && !currentState.code.contains("-")) {
                            ClipboardUtils.copy(context, currentState.code)
                            Toast.makeText(context, totpCopiedMsg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val displayText = if (isSteam) (currentState?.code ?: "------") else (currentState?.code?.chunked(3)?.joinToString(" ") ?: "------")
                        
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = if (isSteam) 4.sp else 2.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(24.dp))
                        CircularProgressIndicator(
                            progress = { currentState?.progress ?: 0f },
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 4.dp,
                            color = if ((currentState?.progress ?: 1f) < 0.2f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }

                if (totpEditState.isEditing && currentState?.decryptedSecret != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    EditTotpSection(item, vaultViewModel, totpEditState)
                }
            }
        },
        confirmButton = {
            DetailActions(onDeleteClick = { vaultViewModel.itemToDelete = item }, onDismiss = { vaultViewModel.dismissDetail() })
        }
    )

    if (showQrDialog) {
        if (currentState?.decryptedSecret != null) {
            val qrContent = constructOtpAuthUri(item, currentState.decryptedSecret)
            val qrBitmap = remember(qrContent) { QrCodeUtils.generateQrCode(qrContent) }
            QrExportDialog(bitmap = qrBitmap, onDismiss = { showQrDialog = false })
        }
    }
}

@Composable
private fun EditTotpSection(
    item: VaultEntry,
    vaultViewModel: VaultViewModel,
    editState: TotpEditState
) {
    LaunchedEffect(editState.secret) {
        if (editState.secret.contains("Steam", ignoreCase = true)) {
            editState.applySteamPreset()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.vault_edit_totp_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        TotpConfigForm(
            secret = editState.secret, onSecretChange = { editState.secret = it },
            period = editState.period, onPeriodChange = { editState.period = it },
            digits = editState.digits, onDigitsChange = { editState.digits = it },
            algorithm = editState.algorithm, onAlgorithmChange = { editState.algorithm = it }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { editState.isEditing = false }) { Text(stringResource(R.string.action_cancel)) }
            Button(onClick = {
                if (editState.secret.isNotBlank()) {
                    try {
                        val encrypted = CryptoManager.encrypt(editState.secret.trim())
                        vaultViewModel.updateVaultEntry(item.copy(
                            totpSecret = encrypted,
                            totpPeriod = editState.period.toIntOrNull() ?: 30,
                            totpDigits = editState.digits.toIntOrNull() ?: 6,
                            totpAlgorithm = editState.algorithm
                        ))
                        editState.isEditing = false
                    } catch (e: Exception) {
                    }
                }
            }) { Text(stringResource(R.string.action_save)) }
        }
    }
}

@Composable
private fun QrExportDialog(bitmap: Bitmap?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.vault_export_qr_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.vault_export_qr_message), style = MaterialTheme.typography.bodyMedium)
                if (bitmap != null) {
                    Card(modifier = Modifier.size(240.dp), shape = RoundedCornerShape(12.dp)) {
                        Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                            Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
    )
}

private fun constructOtpAuthUri(entry: VaultEntry, secret: String): String {
    val type = if (entry.totpAlgorithm.uppercase() == "STEAM") "totp" else "totp"
    val issuer = URLEncoder.encode(entry.category, "UTF-8")
    val label = URLEncoder.encode(entry.title, "UTF-8")
    val secretEncoded = secret.replace(" ", "").uppercase()
    
    return "otpauth://$type/$label?secret=$secretEncoded&issuer=$issuer&period=${entry.totpPeriod}&digits=${entry.totpDigits}&algorithm=${entry.totpAlgorithm}"
}
