package com.aozijx.passly.features.vault.internal

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.crypto.CryptoManager

internal typealias AuthenticateAction =
    (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit

internal class VaultCryptoSupport {

    fun decryptSilently(encryptedData: String?): String? {
        if (encryptedData == null) return null
        if (encryptedData.isEmpty()) return ""
        return runCatching { CryptoManager.decrypt(encryptedData) }.getOrNull()
    }

    fun decryptSingle(
        activity: FragmentActivity,
        encryptedData: String,
        authenticate: AuthenticateAction,
        onResult: (String?) -> Unit
    ) {
        if (encryptedData.isEmpty()) {
            onResult("")
            return
        }

        authenticate(activity, "查看详情", "", null) {
            onResult(runCatching { CryptoManager.decrypt(encryptedData) }.getOrNull())
        }
    }

    fun decryptMultiple(
        activity: FragmentActivity,
        encryptedList: List<String>,
        authenticate: AuthenticateAction,
        onResult: (List<String?>) -> Unit
    ) {
        if (encryptedList.isEmpty()) {
            onResult(emptyList())
            return
        }

        authenticate(activity, "查看详情", "", null) {
            val results = runCatching {
                encryptedList.map { if (it.isEmpty()) "" else CryptoManager.decrypt(it) }
            }.getOrElse {
                encryptedList.map { null }
            }
            onResult(results)
        }
    }

    fun decryptTotpSecret(encrypted: String?): String? {
        return decryptSilently(encrypted)
    }
}
