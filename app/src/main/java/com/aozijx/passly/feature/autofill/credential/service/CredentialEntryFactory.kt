package com.aozijx.passly.feature.autofill.credential.service

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.PasswordCredentialEntry
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
        option: BeginGetPasswordOption,
    ): PasswordCredentialEntry = PasswordCredentialEntry.Builder(
        context = context,
        username = candidate.username,
        pendingIntent = CredentialPendingIntentFactory.createPasswordGetPendingIntent(
            context = context,
            entryId = candidate.candidateId,
        ),
        beginGetPasswordOption = option,
    )
        .setDisplayName(candidate.displayName)
        .setIcon(Icon.createWithResource(context, android.R.drawable.ic_lock_lock))
        .setLastUsedTime(null)
        .setAutoSelectAllowed(false)
        .setDefaultIconPreferredAsSingleProvider(false)
        .build()

}
