@file:Suppress("NewApi")

package com.aozijx.passly.service.autofill.credential

import androidx.credentials.provider.CallingAppInfo

/**
 * Resolves the scope that Passly can authenticate without guessing an origin.
 *
 * A non-null origin means a privileged app (normally a browser) is requesting
 * credentials on behalf of another origin. Reading that origin requires a
 * provider-maintained signing-certificate allowlist. Until Passly ships that
 * allowlist, treating the browser package as the relying party would be unsafe.
 */
internal object CredentialCallingAppResolver {

    fun resolveNativePackage(callingAppInfo: CallingAppInfo?): String? {
        if (callingAppInfo == null || callingAppInfo.isOriginPopulated()) {
            return null
        }
        return callingAppInfo.packageName.takeIf(String::isNotBlank)
    }
}
