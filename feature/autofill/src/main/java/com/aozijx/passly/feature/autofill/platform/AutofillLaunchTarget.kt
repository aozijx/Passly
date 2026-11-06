package com.aozijx.passly.feature.autofill.platform

import android.content.Context
import android.content.Intent

interface AutofillLaunchTarget {
    fun legacyFillIntent(
        context: Context,
        requestId: String,
    ): Intent

    fun credentialResponseIntent(
        context: Context,
        requestId: String,
    ): Intent

    fun accessibilityFillIntent(context: Context): Intent
}

object AutofillHostLaunchContract {
    const val ACTION_LEGACY_FILL = "com.aozijx.passly.autofill.action.LEGACY_FILL"
    const val ACTION_ACCESSIBILITY_FILL = "com.aozijx.passly.autofill.action.ACCESSIBILITY_FILL"
    const val REQUEST_ID = "autofill_request_id"
}

internal object AutofillIntentExtras {
    const val ACTION_CREDENTIAL_GET_PASSWORD = "com.aozijx.passly.ACTION_GET_PASSWORD"
    const val ACTION_CREDENTIAL_UNLOCK = "com.aozijx.passly.ACTION_CREDENTIAL_UNLOCK"
    const val ACTION_CREDENTIAL_CREATE_PASSWORD = "com.aozijx.passly.ACTION_CREATE_PASSWORD"
    const val RESULT_TARGET = "autofill_result_target"
    const val INTERACTION_ID = "autofill_interaction_id"
    const val ENTRY_ID = "autofill_entry_id"
    const val UI_MODE = "autofill_ui_mode"
    const val INLINE_REQUEST = "autofill_inline_request"
    const val PACKAGE_NAME = "autofill_package_name"
    const val WEB_DOMAIN = "autofill_web_domain"
    const val EDITABLE_IDS = "autofill_editable_ids"
    const val USERNAME_IDS = "autofill_username_ids"
    const val PASSWORD_IDS = "autofill_password_ids"
    const val OTP_IDS = "autofill_otp_ids"
}
