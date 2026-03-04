package com.example.poop.ui.screens.vault.utils

import androidx.fragment.app.FragmentActivity
import com.example.poop.util.Logcat
import org.json.JSONArray

/**
 * 保险箱安全验证工具类
 * 集中处理涉及生物识别的加解密及复杂字段序列化逻辑
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
            onSuccess = {
                val results = encryptedTexts.map { text ->
                    try {
                        val iv = CryptoManager.getIvFromCipherText(text)
                        val cipher = iv?.let { CryptoManager.getDecryptCipher(it) }
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

    /**
     * 单个解密（需身份验证）
     */
    fun decryptSingle(
        activity: FragmentActivity,
        encryptedText: String,
        title: String = "验证身份",
        subtitle: String = "请验证以继续",
        onSuccess: (String?) -> Unit
    ) {
        decryptMultiple(activity, listOf(encryptedText), title, subtitle) { results ->
            onSuccess(results.firstOrNull())
        }
    }

    /**
     * 批量加密（需身份验证）
     */
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
                    try {
                        val cipher = CryptoManager.getEncryptCipher()
                        if (cipher != null) {
                            CryptoManager.encrypt(text, cipher)
                        } else ""
                    } catch (e: Exception) {
                        Logcat.e("VaultSecurity", "Encryption failed", e)
                        ""
                    }
                }
                onSuccess(results)
            }
        )
    }

    /**
     * 将恢复码文本（换行分隔）序列化为 JSON 数组字符串
     */
    fun serializeRecoveryCodes(rawText: String): String {
        val codes = rawText.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val jsonArray = JSONArray()
        codes.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    /**
     * 将 JSON 数组字符串反序列化为恢复码列表
     */
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
