package com.aozijx.passly.feature.autofill.platform

import android.app.PendingIntent

object AutofillPendingIntentPolicy {
    const val ACTIVITY_FLAGS: Int = PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    fun legacyFillRequestId(interactionId: String, entryId: String): String =
        "autofill-fill:$interactionId:$entryId"

    fun legacyInteractionRequestId(interactionId: String): String =
        "autofill-interaction:$interactionId"

    fun credentialGetRequestId(entryId: String): String = "password-get:$entryId"

    fun credentialGetRequestCode(entryId: String): Int = entryId.hashCode()

    const val CREDENTIAL_CREATE_REQUEST_CODE: Int = 0x30000
    const val CREDENTIAL_UNLOCK_REQUEST_CODE: Int = 0x20000
}
