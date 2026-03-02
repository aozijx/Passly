package com.example.poop.ui.screens.vault.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.poop.data.VaultItem
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.util.BiometricHelper
import com.example.poop.util.ClipboardUtils
import com.example.poop.util.CryptoManager

@Composable
fun VaultDetailDialog(
    activity: FragmentActivity,
    item: VaultItem,
    viewModel: VaultViewModel
) {
    val context = LocalContext.current
    val isRevealed = viewModel.isItemRevealed(item.id)
    val decryptedData = viewModel.getDecryptedData(item.id)

    AlertDialog(
        onDismissRequest = { viewModel.dismissDetail() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        getCategoryIcon(item.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(item.title, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                // 分类项：横向布局且支持编辑
                CategoryItem(viewModel, item)

                DetailItem(
                    label = "账号",
                    value = if (isRevealed) decryptedData?.first ?: "" else "••••••••",
                    isRevealed = isRevealed,
                    onCopy = {
                        val username = decryptedData?.first ?: ""
                        ClipboardUtils.copy(context, username)
                        Toast.makeText(context, "账号已复制", Toast.LENGTH_SHORT).show()
                    }
                )

                DetailItem(
                    label = "密码",
                    value = if (isRevealed) decryptedData?.second ?: "" else "••••••••",
                    isRevealed = isRevealed,
                    onCopy = {
                        val password = decryptedData?.second ?: ""
                        ClipboardUtils.copy(context, password)
                        Toast.makeText(context, "密码已复制", Toast.LENGTH_SHORT).show()
                    }
                )

                if (!isRevealed) {
                    Button(
                        onClick = {
                            decryptMultiple(activity, listOf(item.username, item.password)) { results ->
                                if (results.size >= 2) {
                                    viewModel.setRevealedData(item.id, results[0], results[1])
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("点击验证并显示")
                    }
                } else {
                    TextButton(
                        onClick = { viewModel.clearRevealedData(item.id) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("隐藏敏感信息")
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { viewModel.requestDelete(item) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除")
                }

                TextButton(
                    onClick = { viewModel.dismissDetail() }
                ) { 
                    Text("关闭") 
                }
            }
        },
        dismissButton = null
    )
}

@Composable
private fun CategoryItem(viewModel: VaultViewModel, item: VaultItem) {
    if (viewModel.isEditingCategory) {
        OutlinedTextField(
            value = viewModel.editedCategory,
            onValueChange = { viewModel.editedCategory = it },
            label = { Text("修改分类") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { viewModel.saveCategoryEdit() }) {
                    Icon(Icons.Default.Check, contentDescription = "保存", tint = MaterialTheme.colorScheme.primary)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { viewModel.startEditingCategory() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "分类",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = item.category,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    isRevealed: Boolean = true,
    onCopy: (() -> Unit)? = null
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = if (isRevealed || label == "分类") 0.sp else 2.sp
                ),
                modifier = Modifier.weight(1f)
            )
            if (isRevealed && onCopy != null) {
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private fun decryptMultiple(activity: FragmentActivity, encryptedTexts: List<String>, onSuccess: (List<String>) -> Unit) {
    if (encryptedTexts.isEmpty()) return
    BiometricHelper.authenticate(
        activity = activity,
        title = "查看敏感信息",
        subtitle = "验证身份以解锁详情",
        onSuccess = {
            val results = encryptedTexts.map { encryptedText ->
                val iv = CryptoManager.getIvFromCipherText(encryptedText)
                if (iv != null) {
                    CryptoManager.getDecryptCipher(iv)?.let { cipher ->
                        CryptoManager.decrypt(encryptedText, cipher)
                    } ?: ""
                } else ""
            }
            onSuccess(results)
        }
    )
}
