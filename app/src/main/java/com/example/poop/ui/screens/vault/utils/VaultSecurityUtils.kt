package com.example.poop.ui.screens.vault.utils

import androidx.fragment.app.FragmentActivity
import com.example.poop.util.Logcat
import org.json.JSONArray

/**
 * 保险箱安全验证工具类
 */
object VaultSecurityUtils {

    /**
     * 批量解密（需身份验证）
     */
    fun decryptMultiple(
        activity: FragmentActivity,
        encryptedTexts: List<String>,
        title: String = "验证身份",
        subtitle: String = "请验证以继续",
        onFailure: (() -> Unit)? = null,
        onSuccess: (List<String?>) -> Unit
    ) {
        if (encryptedTexts.isEmpty()) {
            onSuccess(emptyList())
            return
        }

        BiometricHelper.authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onError = { _ -> onFailure?.invoke() },
            onSuccess = {
                val results = encryptedTexts.map { text ->
                    try {
                        val iv = CryptoManager.getIvFromCipherText(text)
                        val cipher = iv?.let { CryptoManager.getDecryptCipher(it, isSilent = false) }
                        if (cipher != null) {
                            CryptoManager.decrypt(text, cipher)
                        } else null
                    } catch (e: Exception) {
                        Logcat.e("VaultSecurity", "Decryption failed", e)
                        null
                    }
                }
                onSuccess(results)
            }
        )
    }

    fun decryptSingle(
        activity: FragmentActivity,
        encryptedText: String,
        title: String = "验证身份",
        subtitle: String = "请验证以继续",
        onFailure: (() -> Unit)? = null,
        onSuccess: (String?) -> Unit
    ) {
        decryptMultiple(activity, listOf(encryptedText), title, subtitle, onFailure) { results ->
            onSuccess(results.firstOrNull())
        }
    }

    fun encryptMultiple(
        activity: FragmentActivity,
        texts: List<String>,
        title: String = "加密数据",
        subtitle: String = "验证以保护您的信息",
        onSuccess: (List<String>) -> Unit
    ) {
        BiometricHelper.authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = {
                val results = texts.map { text ->
                    CryptoManager.encrypt(text, isSilent = false) ?: ""
                }
                onSuccess(results)
            }
        )
    }

    fun serializeRecoveryCodes(rawText: String): String {
        val codes = rawText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val jsonArray = JSONArray()
        codes.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    fun deserializeRecoveryCodes(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val jsonArray = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            Logcat.e("VaultSecurity", "Failed to deserialize recovery codes", e)
            emptyList()
        }
    }
}
