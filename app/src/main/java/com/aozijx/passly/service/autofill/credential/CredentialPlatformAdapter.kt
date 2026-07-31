@file:Suppress("NewApi")

package com.aozijx.passly.service.autofill.credential

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.PasswordCredentialEntry
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.autofill.model.FieldDescriptor
import com.aozijx.passly.core.autofill.model.FillRequestSource
import com.aozijx.passly.core.autofill.model.InternalFillRequest
import com.aozijx.passly.core.autofill.model.InternalFillResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Credential 平台适配器：负责 Android CredentialManager API 与核心 FillPipeline 之间的双向转换。
 *
 * - [buildRequest]：凭据查询参数 → InternalFillRequest
 * - [buildPasswordEntries]：InternalFillResponse → PasswordCredentialEntry 列表
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
            fields = listOf(
                FieldDescriptor(
                    viewId = "credential_username",
                    autofillHints = listOf("USERNAME"),
                ),
                FieldDescriptor(
                    viewId = "credential_password",
                    autofillHints = listOf("PASSWORD"),
                ),
            ),
            source = FillRequestSource.CREDENTIAL_MANAGER,
        )
    }

    fun buildPasswordEntries(
        response: InternalFillResponse,
        context: Context,
        option: BeginGetPasswordOption,
    ): List<PasswordCredentialEntry> {
        val candidates = response.candidates.filter {
            it.username.isNotBlank() &&
                    (option.allowedUserIds.isEmpty() || it.username in option.allowedUserIds)
        }
        if (candidates.isEmpty()) {
            AppTelemetry.d(TAG, "No candidates for password entries")
            return emptyList()
        }

        return candidates.map { candidate ->
            CredentialEntryFactory.buildPasswordEntry(
                context = context,
                candidate = candidate,
                option = option,
            )
        }
    }
}
