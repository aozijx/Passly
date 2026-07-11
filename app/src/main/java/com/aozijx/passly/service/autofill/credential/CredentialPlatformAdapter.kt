@file:Suppress("NewApi")

package com.aozijx.passly.service.autofill.credential

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.aozijx.passly.core.autofill.model.InternalFillRequest
import com.aozijx.passly.core.autofill.model.InternalFillResponse
import com.aozijx.passly.core.log.Logcat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Credential 平台适配器：负责 Android CredentialManager API 与核心 FillPipeline 之间的双向转换。
 *
 * - [buildRequest]：凭据查询参数 → InternalFillRequest
 * - [buildPasswordEntries]：InternalFillResponse → PasswordCredentialEntry 列表
 * - [buildPasskeyEntries]：InternalFillResponse → PublicKeyCredentialEntry 列表
 *
 * CredentialManager API 版本升级时，只需替换此适配器。
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Singleton
class CredentialPlatformAdapter @Inject constructor() {

    companion object {
        private const val TAG = "CredAdapter"
    }

    fun buildRequest(packageName: String): InternalFillRequest {
        return InternalFillRequest(
            parentPackage = packageName,
            webDomain = null,
            fields = emptyList(),
        )
    }

    fun buildPasswordEntries(
        response: InternalFillResponse,
        context: Context,
        packageName: String,
        option: BeginGetPasswordOption,
    ): List<PasswordCredentialEntry> {
        val candidates = response.candidates
        if (candidates.isEmpty()) {
            Logcat.d(TAG, "No candidates for password entries")
            return emptyList()
        }

        return candidates.map { candidate ->
            CredentialEntryFactory.buildPasswordEntry(
                context = context,
                candidate = candidate,
                packageName = packageName,
                option = option,
            )
        }
    }

    fun buildPasskeyEntries(
        response: InternalFillResponse,
        context: Context,
        packageName: String,
        option: BeginGetPublicKeyCredentialOption,
    ): List<PublicKeyCredentialEntry> {
        val candidates = response.candidates
        if (candidates.isEmpty()) {
            Logcat.d(TAG, "No candidates for passkey entries")
            return emptyList()
        }

        return candidates.map { candidate ->
            CredentialEntryFactory.buildPasskeyEntry(
                context = context,
                candidate = candidate,
                packageName = packageName,
                option = option,
            )
        }
    }
}