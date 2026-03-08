package com.example.poop.ui.screens.vault.types.totp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.ui.screens.vault.utils.CryptoManager
import com.example.poop.ui.screens.vault.utils.TwoFAUtils
import com.example.poop.util.Logcat
import kotlinx.coroutines.delay

data class TotpState(
    val code: String = "------",
    val progress: Float = 1f,
    val decryptedSecret: String? = null
)

/**
 * 统一管理 TOTP 的解密、刷新和进度计算逻辑
 */
@Composable
fun rememberTotpState(
    entry: VaultEntry,
    viewModel: VaultViewModel,
    refreshKey: Any? = null,
    autoMigrate: Boolean = false
): TotpState {
    var code by remember { mutableStateOf("------") }
    var progress by remember { mutableFloatStateOf(1f) }
    var decryptedSecret by remember { mutableStateOf<String?>(null) }

    // 1. 解密逻辑 (双密钥尝试 + 自动升级)
    LaunchedEffect(entry.id, entry.totpSecret, refreshKey) {
        val encrypted = entry.totpSecret ?: return@LaunchedEffect
        try {
            val iv = CryptoManager.getIvFromCipherText(encrypted) ?: return@LaunchedEffect

            // 优先尝试免验证密钥
            var cipher = CryptoManager.getDecryptCipher(iv, isSilent = true)
            var decrypted = cipher?.let { CryptoManager.decrypt(encrypted, it) }

            // 失败则尝试安全密钥
            if (decrypted == null) {
                cipher = CryptoManager.getDecryptCipher(iv, isSilent = false)
                decrypted = cipher?.let { CryptoManager.decrypt(encrypted, it) }
                
                // 自动升级逻辑 (仅在指定且解密成功时执行)
                if (autoMigrate && decrypted != null) {
                    val newCipher = CryptoManager.getEncryptCipher(isSilent = true)
                    if (newCipher != null) {
                        val reEncrypted = CryptoManager.encrypt(decrypted, newCipher)
                        viewModel.updateVaultEntry(entry.copy(totpSecret = reEncrypted))
                        Logcat.i("TotpState", "Entry ${entry.id} migrated to silent key")
                    }
                }
            }
            decryptedSecret = decrypted
        } catch (e: Exception) {
            Logcat.e("TotpState", "Decryption failed", e)
        }
    }

    // 2. 验证码和进度刷新逻辑
    val isSteam = remember(entry.totpAlgorithm) { entry.totpAlgorithm.uppercase() == "STEAM" }
    LaunchedEffect(decryptedSecret, entry.totpAlgorithm, entry.totpDigits, entry.totpPeriod) {
        val secret = decryptedSecret ?: return@LaunchedEffect
        while (true) {
            val period = entry.totpPeriod.coerceAtLeast(1)
            val currentTime = System.currentTimeMillis() / 1000
            val remaining = period - (currentTime % period)
            progress = remaining.toFloat() / period

            code = TwoFAUtils.generateTotp(
                secret = secret,
                digits = if (isSteam) 5 else entry.totpDigits,
                period = entry.totpPeriod,
                algorithm = entry.totpAlgorithm
            )
            delay(500)
        }
    }

    return TotpState(code, progress, decryptedSecret)
}
