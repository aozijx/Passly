package com.aozijx.passly.feature.autofill.credential.service

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchExtras
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
        interactionId: AutofillInteractionId,
        entryId: EntryId,
    ): PendingIntent {
        val requestId = AutofillPendingIntentPolicy.credentialGetRequestId(
            interactionId.value,
            entryId.value,
        )
        val intent = launchTarget.credentialResponseIntent(context, requestId)
            .setAction(ModernCredentialService.ACTION_GET_PASSWORD)
            // PendingIntent identity ignores extras. Identifier keeps two UUIDs
            // distinct even in the unlikely event of an Int hash collision.
            .setIdentifier(requestId)
            // The entry id is only an opaque candidate pointer. Calling-app
            // identity always comes from the system-injected final request.
            .putExtra(AutofillLaunchExtras.INTERACTION_ID, interactionId.value)
            .putExtra(AutofillLaunchExtras.ENTRY_ID, entryId.value)

        return PendingIntent.getActivity(
            context,
            AutofillPendingIntentPolicy.credentialGetRequestCode(
                interactionId.value,
                entryId.value,
            ),
            intent,
            AutofillPendingIntentPolicy.ACTIVITY_FLAGS,
        )
    }

    fun createPasswordCreatePendingIntent(
        context: Context,
        interactionId: AutofillInteractionId,
    ): PendingIntent {
        val requestId = AutofillPendingIntentPolicy.credentialCreateRequestId(interactionId.value)
        val intent = launchTarget.credentialResponseIntent(context, requestId)
            .setAction(ModernCredentialService.ACTION_CREATE_PASSWORD)
            .setIdentifier(requestId)
            .putExtra(AutofillLaunchExtras.INTERACTION_ID, interactionId.value)
        return PendingIntent.getActivity(
            context,
            requestId.hashCode(),
            intent,
            AutofillPendingIntentPolicy.ACTIVITY_FLAGS,
        )
    }

    fun createUnlockPendingIntent(
        context: Context,
        interactionId: AutofillInteractionId,
    ): PendingIntent {
        val requestId = AutofillPendingIntentPolicy.credentialUnlockRequestId(interactionId.value)
        val intent = launchTarget.credentialResponseIntent(context, requestId)
            .setAction(ModernCredentialService.ACTION_UNLOCK)
            .setIdentifier(requestId)
            .putExtra(AutofillLaunchExtras.INTERACTION_ID, interactionId.value)
        return PendingIntent.getActivity(
            context,
            requestId.hashCode(),
            intent,
            AutofillPendingIntentPolicy.ACTIVITY_FLAGS,
        )
    }
}
