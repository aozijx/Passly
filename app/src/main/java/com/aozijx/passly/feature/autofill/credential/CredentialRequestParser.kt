package com.aozijx.passly.feature.autofill.credential

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.CreateCredentialUnsupportedException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.PendingIntentHandler
import com.aozijx.passly.service.autofill.credential.CredentialCallingAppResolver
import com.aozijx.passly.service.autofill.credential.ModernCredentialService

internal sealed interface PasswordGetParseResult {
    data class Ready(
        val entryId: String,
        val packageName: String,
        val option: GetPasswordOption,
    ) : PasswordGetParseResult
    data class Failed(val exception: GetCredentialException) : PasswordGetParseResult
}

internal sealed interface UnlockParseResult {
    data class Ready(val request: BeginGetCredentialRequest) : UnlockParseResult
    data class Failed(val exception: GetCredentialException) : UnlockParseResult
}

internal sealed interface PasswordCreateParseResult {
    data class Ready(
        val packageName: String,
        val username: String,
        val password: String,
    ) : PasswordCreateParseResult
    data class Failed(val exception: CreateCredentialException) : PasswordCreateParseResult
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal object CredentialRequestParser {
    fun parsePasswordGet(sourceIntent: Intent): PasswordGetParseResult {
        val providerRequest =
            PendingIntentHandler.retrieveProviderGetCredentialRequest(sourceIntent)
                ?: return PasswordGetParseResult.Failed(
                    GetCredentialUnknownException("Missing system get request")
                )
        val option = providerRequest.credentialOptions.singleOrNull() as? GetPasswordOption
            ?: return PasswordGetParseResult.Failed(
                GetCredentialUnsupportedException("Selected entry is not a password request")
            )
        val packageName = CredentialCallingAppResolver.resolveNativePackage(
            providerRequest.callingAppInfo
        ) ?: return PasswordGetParseResult.Failed(
            GetCredentialUnsupportedException("Privileged origin requests are not configured")
        )
        val entryId = sourceIntent.getStringExtra(ModernCredentialService.EXTRA_ENTRY_ID).orEmpty()
        if (entryId.isBlank()) {
            return PasswordGetParseResult.Failed(
                GetCredentialUnknownException("Missing selected credential id")
            )
        }
        return PasswordGetParseResult.Ready(entryId, packageName, option)
    }

    fun parseUnlock(sourceIntent: Intent): UnlockParseResult {
        val request = PendingIntentHandler.retrieveBeginGetCredentialRequest(sourceIntent)
            ?: return UnlockParseResult.Failed(
                GetCredentialUnknownException("Missing system begin-get request")
            )
        if (CredentialCallingAppResolver.resolveNativePackage(request.callingAppInfo) == null) {
            return UnlockParseResult.Failed(
                GetCredentialUnsupportedException("Privileged origin requests are not configured")
            )
        }
        return UnlockParseResult.Ready(request)
    }

    fun parsePasswordCreate(sourceIntent: Intent): PasswordCreateParseResult {
        val providerRequest =
            PendingIntentHandler.retrieveProviderCreateCredentialRequest(sourceIntent)
                ?: return PasswordCreateParseResult.Failed(
                    CreateCredentialUnknownException("Missing system create request")
                )
        val request = providerRequest.callingRequest as? CreatePasswordRequest
            ?: return PasswordCreateParseResult.Failed(
                CreateCredentialUnsupportedException(
                    "Selected entry is not a password create request"
                )
            )
        val packageName = CredentialCallingAppResolver.resolveNativePackage(
            providerRequest.callingAppInfo
        ) ?: return PasswordCreateParseResult.Failed(
            CreateCredentialUnsupportedException("Privileged origin requests are not configured")
        )
        return PasswordCreateParseResult.Ready(
            packageName = packageName,
            username = request.id,
            password = request.password,
        )
    }
}
