package com.aozijx.passly.feature.autofill.credential.service

import androidx.credentials.provider.CallingAppInfo

/**
 * Resolves the scope that Passly can authenticate.
 *
 * Note: origin access is restricted by Android system. For now, we only
 * support native package identification.
 */
internal object CredentialCallingAppResolver {

    fun resolveNativePackage(callingAppInfo: CallingAppInfo?): String? {
        if (callingAppInfo == null || callingAppInfo.isOriginPopulated()) return null
        return callingAppInfo.packageName.takeIf(String::isNotBlank)
    }
}
