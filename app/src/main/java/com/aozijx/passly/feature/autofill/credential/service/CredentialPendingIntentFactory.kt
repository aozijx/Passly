package com.aozijx.passly.feature.autofill.credential.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.aozijx.passly.feature.autofill.credential.CredentialResponseActivity

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal object CredentialPendingIntentFactory {

    fun createPasswordGetPendingIntent(
        context: Context,
        entryId: String,
    ): PendingIntent {
        val intent = Intent(context, CredentialResponseActivity::class.java)
            .setAction(ModernCredentialService.ACTION_GET_PASSWORD)
            // PendingIntent identity ignores extras. Identifier keeps two UUIDs
            // distinct even in the unlikely event of an Int hash collision.
            .setIdentifier("password-get:$entryId")
            // The entry id is only an opaque candidate pointer. Calling-app
            // identity always comes from the system-injected final request.
            .putExtra(ModernCredentialService.EXTRA_ENTRY_ID, entryId)

        return PendingIntent.getActivity(
            context,
            entryId.hashCode(),
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun createPasswordCreatePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, CredentialResponseActivity::class.java)
            .setAction(ModernCredentialService.ACTION_CREATE_PASSWORD)
        return PendingIntent.getActivity(
            context,
            0x30000,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
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
