package com.example.poop.ui.screens.vault.autofill

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.utils.CryptoManager
import com.example.poop.util.Logcat
import kotlinx.coroutines.launch

/**
 * 统一管理自动填充条目的解密和自动密钥升级逻辑
 */
@Composable
fun rememberDecryptedAutofillData(
    entry: VaultEntry,
    onReadyToUpgrade: () -> Unit // 用于触发UI层（如弹窗）进行验证
): Pair<String?, String?> {
    var username by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(entry.id) {
        // 1. 优先尝试免验证密钥（针对刚抓取的数据）
        try {
            val userIv = CryptoManager.getIvFromCipherText(entry.username)
            val passIv = CryptoManager.getIvFromCipherText(entry.password)

            val userCipher = userIv?.let { CryptoManager.getDecryptCipher(it, isSilent = true) }
            val passCipher = passIv?.let { CryptoManager.getDecryptCipher(it, isSilent = true) }

            if (userCipher != null && passCipher != null) {
                val decryptedUser = CryptoManager.decrypt(entry.username, userCipher)
                val decryptedPass = CryptoManager.decrypt(entry.password, passCipher)

                if (decryptedUser != null && decryptedPass != null) {
                    username = decryptedUser
                    password = decryptedPass
                    // 解密成功，说明是免验证数据，通知UI层可以准备升级了
                    onReadyToUpgrade()
                    return@LaunchedEffect
                }
            }
        } catch (e: Exception) {
            Logcat.e("AutofillState", "Silent decryption failed", e)
        }

        // 2. 如果静默解密失败，则尝试安全密钥（此逻辑应由 UI 层的生物识别回调触发）
        // 这里我们只返回 null，UI 层根据 null 状态决定是否显示“验证”按钮
        username = null
        password = null
    }

    return Pair(username, password)
}

/**
 * 升级密钥：将免验证数据重新加密为安全数据
 */
fun upgradeToSecureEntry(
    entry: VaultEntry,
    viewModel: VaultViewModel
) {
    viewModel.viewModelScope.launch {
        try {
            // 1. 先用免验证密钥解密出原始数据
            val userIv = CryptoManager.getIvFromCipherText(entry.username)
            val passIv = CryptoManager.getIvFromCipherText(entry.password)
            val userCipher = userIv?.let { CryptoManager.getDecryptCipher(it, isSilent = true) }
            val passCipher = passIv?.let { CryptoManager.getDecryptCipher(it, isSilent = true) }

            if (userCipher != null && passCipher != null) {
                val rawUser = CryptoManager.decrypt(entry.username, userCipher)
                val rawPass = CryptoManager.decrypt(entry.password, passCipher)

                if (rawUser != null && rawPass != null) {
                    // 2. 使用安全密钥重新加密
                    val secureUser = CryptoManager.encrypt(rawUser, isSilent = false)
                    val securePass = CryptoManager.encrypt(rawPass, isSilent = false)

                    if (secureUser != null && securePass != null) {
                        // 3. 更新数据库条目
                        viewModel.updateVaultEntry(
                            entry.copy(
                                username = secureUser,
                                password = securePass
                            )
                        )
                        Logcat.i("AutofillState", "Entry ${entry.id} migrated to SECURE key.")
                    }
                }
            }
        } catch (e: Exception) {
            Logcat.e("AutofillState", "Upgrade to secure entry failed", e)
        }
    }
}
