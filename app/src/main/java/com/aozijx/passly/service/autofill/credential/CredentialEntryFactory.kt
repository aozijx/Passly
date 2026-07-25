package com.aozijx.passly.service.autofill.credential

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.aozijx.passly.core.autofill.model.ResolvedCandidate

/**
 * Credential 条目工厂：负责构建 Credential Manager 所需的条目对象。
 *
 * 输入为 Core 层的 [ResolvedCandidate]，由 [CredentialPlatformAdapter] 传入。
 * 不直接依赖 domain 层的 [com.aozijx.passly.domain.model.credential.CredentialCandidate]。
 */
internal object CredentialEntryFactory {

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun buildPasswordEntry(
        context: Context,
        candidate: ResolvedCandidate,
        packageName: String,
        option: BeginGetPasswordOption,
    ): PasswordCredentialEntry = PasswordCredentialEntry.Builder(
        context = context,
        username = candidate.username,
        pendingIntent = CredentialPendingIntentFactory.createPendingIntent(
            context = context,
            action = ModernCredentialService.ACTION_GET_PASSWORD,
            entryId = candidate.candidateId,
            entryTitle = candidate.displayName,
            username = candidate.username,
            callingPackage = packageName,
            associatedDomain = candidate.associatedDomain,
        ),
        beginGetPasswordOption = option,
    )
        .setDisplayName(candidate.displayName)
        .setIcon(Icon.createWithResource(context, android.R.drawable.ic_lock_lock))
        .setLastUsedTime(null)
        .setAutoSelectAllowed(false)
        .setDefaultIconPreferredAsSingleProvider(false)
        .build()

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun buildPasskeyEntry(
        context: Context,
        candidate: ResolvedCandidate,
        packageName: String,
        option: BeginGetPublicKeyCredentialOption,
    ): PublicKeyCredentialEntry = PublicKeyCredentialEntry.Builder(
        context = context,
        username = candidate.username,
        pendingIntent = CredentialPendingIntentFactory.createPendingIntent(
            context = context,
            action = ModernCredentialService.ACTION_GET_PASSKEY,
            entryId = candidate.candidateId,
            entryTitle = candidate.displayName,
            username = candidate.username,
            callingPackage = packageName,
            associatedDomain = candidate.associatedDomain,
        ),
        beginGetPublicKeyCredentialOption = option,
    )
        .setDisplayName(candidate.displayName)
        .setIcon(Icon.createWithResource(context, android.R.drawable.ic_lock_lock))
        .setLastUsedTime(null)
        .setAutoSelectAllowed(false)
        .build()
}