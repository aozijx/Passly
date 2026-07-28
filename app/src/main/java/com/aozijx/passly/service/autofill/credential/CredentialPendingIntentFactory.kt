@file:Suppress("NewApi")

package com.aozijx.passly.service.autofill.credential

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.aozijx.passly.feature.autofill.credential.CredentialResponseActivity

internal object CredentialPendingIntentFactory {

    fun createPendingIntent(
        context: Context,
        action: String,
        entryId: String,
        callingPackage: String,
    ): PendingIntent {
        val data = Bundle().apply {
            putString(ModernCredentialService.EXTRA_ENTRY_ID, entryId)
            putString(ModernCredentialService.EXTRA_PACKAGE_NAME, callingPackage)
        }

        val intent = Intent(context, CredentialResponseActivity::class.java)
            .setAction(action)
            .putExtra(ModernCredentialService.EXTRA_CREDENTIAL_DATA, data)

        return PendingIntent.getActivity(
            context,
            entryId.hashCode(),
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun createUnlockPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, CredentialResponseActivity::class.java)
            .setAction(ModernCredentialService.ACTION_UNLOCK)
        return PendingIntent.getActivity(
            context,
            0x20000,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
