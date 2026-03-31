package com.example.poop.core.designsystem.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.poop.core.crypto.CryptoManager
import com.example.poop.data.model.VaultEntry
import com.example.poop.features.vault.VaultViewModel
import com.example.poop.util.Logcat
import kotlinx.coroutines.launch

/**
 * 升级密钥：将免验证数据重新加密为安全数据
 */
fun upgradeToSecureEntry(
    entry: VaultEntry,
    viewModel: VaultViewModel
) {
    viewModel.viewModelScope.launch {
        try {
            val rawUser = CryptoManager.decrypt(entry.username)
            val rawPass = CryptoManager.decrypt(entry.password)

            if (rawUser.isNotEmpty() && rawPass.isNotEmpty()) {
                val secureUser = CryptoManager.encrypt(rawUser)
                val securePass = CryptoManager.encrypt(rawPass)

                viewModel.updateVaultEntry(
                    entry.copy(
                        username = secureUser,
                        password = securePass
                    )
                )
                Logcat.i("AutofillState", "Entry ${entry.id} migrated/refreshed.")
            }
        } catch (e: Exception) {
            Logcat.e("AutofillState", "Upgrade to secure entry failed", e)
        }
    }
}

/**
 * [Composable] 辅助函数：尝试静默解密自动填充的数据
 * 返回 Pair(解密后的用户名, 解密后的密码)
 */
@Composable
fun rememberDecryptedAutofillData(
    entry: VaultEntry,
    onReadyToUpgrade: () -> Unit
): Pair<String?, String?> {
    var decryptedUser by remember(entry.id) { mutableStateOf<String?>(null) }
    var decryptedPass by remember(entry.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(entry.id) {
        try {
            val user = CryptoManager.decrypt(entry.username)
            val pass = CryptoManager.decrypt(entry.password)
            
            decryptedUser = user
            decryptedPass = pass
            onReadyToUpgrade()
        } catch (e: Exception) {
            Logcat.d("AutofillState", "Silent decryption skipped for entry ${entry.id}")
        }
    }

    return Pair(decryptedUser, decryptedPass)
}
