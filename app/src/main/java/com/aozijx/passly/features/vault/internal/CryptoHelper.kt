package com.aozijx.passly.features.vault.internal

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.crypto.CryptoAccess

internal typealias AuthenticateAction =
    (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit

internal class CryptoHelper {

    fun decryptSingle(
        activity: FragmentActivity,
        encryptedData: String,
        authenticate: AuthenticateAction,
        onResult: (String?) -> Unit
    ) {
        if (encryptedData.isEmpty()) { onResult(""); return }
        authenticate(activity, "查看详情", "", null) {
            onResult(CryptoAccess.decryptOrNull(encryptedData))
        }
    }

    fun decryptMultiple(
        activity: FragmentActivity,
        encryptedList: List<String>,
        authenticate: AuthenticateAction,
        onResult: (List<String?>) -> Unit
    ) {
        if (encryptedList.isEmpty()) { onResult(emptyList()); return }
        authenticate(activity, "查看详情", "", null) {
            onResult(encryptedList.map { CryptoAccess.decryptOrNull(it) })
        }
    }

    // Restricted usage: only for TOTP auto-refresh/preview flow.
    fun decryptTotpSecret(encrypted: String?): String? = CryptoAccess.decryptOrNull(encrypted)
}
