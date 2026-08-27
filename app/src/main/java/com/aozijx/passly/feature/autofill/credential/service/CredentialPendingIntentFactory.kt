package com.aozijx.passly.feature.autofill.credential.service

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchTarget
import com.aozijx.passly.feature.autofill.platform.AutofillPendingIntentPolicy
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Singleton
class CredentialPendingIntentFactory @Inject constructor(
    private val launchTarget: AutofillLaunchTarget,
) {

    fun createPasswordGetPendingIntent(
        context: Context,
        entryId: String,
    ): PendingIntent {
        val requestId = AutofillPendingIntentPolicy.credentialGetRequestId(entryId)
        val intent = launchTarget.credentialResponseIntent(context, requestId)
            .setAction(ModernCredentialService.ACTION_GET_PASSWORD)
            // PendingIntent identity ignores extras. Identifier keeps two UUIDs
            // distinct even in the unlikely event of an Int hash collision.
            .setIdentifier(requestId)
            // The entry id is only an opaque candidate pointer. Calling-app
            // identity always comes from the system-injected final request.
            .putExtra(ModernCredentialService.EXTRA_ENTRY_ID, entryId)

        return PendingIntent.getActivity(
            context,
            AutofillPendingIntentPolicy.credentialGetRequestCode(entryId),
            intent,
            AutofillPendingIntentPolicy.ACTIVITY_FLAGS,
        )
    }

    fun createPasswordCreatePendingIntent(context: Context): PendingIntent {
        val intent = launchTarget.credentialResponseIntent(
            context,
            ModernCredentialService.ACTION_CREATE_PASSWORD,
        )
            .setAction(ModernCredentialService.ACTION_CREATE_PASSWORD)
        return PendingIntent.getActivity(
            context,
            AutofillPendingIntentPolicy.CREDENTIAL_CREATE_REQUEST_CODE,
            intent,
            AutofillPendingIntentPolicy.ACTIVITY_FLAGS,
        )
    }

    fun createUnlockPendingIntent(context: Context): PendingIntent {
        val intent = launchTarget.credentialResponseIntent(
            context,
            ModernCredentialService.ACTION_UNLOCK,
        )
            .setAction(ModernCredentialService.ACTION_UNLOCK)
        return PendingIntent.getActivity(
            context,
            AutofillPendingIntentPolicy.CREDENTIAL_UNLOCK_REQUEST_CODE,
            intent,
            AutofillPendingIntentPolicy.ACTIVITY_FLAGS,
        )
    }
}
