package com.aozijx.passly.features.vault.internal

import androidx.fragment.app.FragmentActivity

internal typealias AuthenticateAction =
    (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit

internal class CryptoHelper {

    fun decryptSingle(
        activity: FragmentActivity,
        data: String,
        authenticate: AuthenticateAction,
        onResult: (String?) -> Unit
    ) {
        if (data.isEmpty()) { onResult(""); return }
        authenticate(activity, "查看详情", "", null) {
            onResult(data)
        }
    }

    fun decryptMultiple(
        activity: FragmentActivity,
        dataList: List<String>,
        authenticate: AuthenticateAction,
        onResult: (List<String?>) -> Unit
    ) {
        if (dataList.isEmpty()) { onResult(emptyList()); return }
        authenticate(activity, "查看详情", "", null) {
            onResult(dataList)
        }
    }

    fun decryptTotpSecret(value: String?): String? = value
}
