package com.aozijx.passly.feature.autofill.platform

import android.app.PendingIntent

object AutofillPendingIntentPolicy {
    const val ACTIVITY_FLAGS: Int = PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    fun legacyFillRequestId(interactionId: String, entryId: String): String =
        "autofill-fill:$interactionId:$entryId"

    fun legacyInteractionRequestId(interactionId: String): String =
        "autofill-interaction:$interactionId"

    fun legacySaveRequestId(token: String): String =
        "autofill-save:$token"

    fun credentialGetRequestId(interactionId: String, entryId: String): String =
        "password-get:$interactionId:$entryId"

    fun credentialGetRequestCode(interactionId: String, entryId: String): Int =
        credentialGetRequestId(interactionId, entryId).hashCode()

    fun credentialCreateRequestId(interactionId: String): String =
        "password-create:$interactionId"

    fun credentialUnlockRequestId(interactionId: String): String =
        "password-unlock:$interactionId"
}
