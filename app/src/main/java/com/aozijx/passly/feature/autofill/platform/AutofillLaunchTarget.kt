package com.aozijx.passly.feature.autofill.platform

import android.content.Context
import android.content.Intent

interface AutofillLaunchTarget {
    fun legacyFillIntent(context: Context, requestId: String): Intent
    fun legacySaveIntent(context: Context, requestId: String): Intent
    fun credentialResponseIntent(context: Context, requestId: String): Intent
    fun accessibilityFillIntent(context: Context): Intent
}

object AutofillLaunchExtras {
    const val REQUEST_ID = "autofill_request_id"
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
    const val LEGACY_SAVE_TOKEN = "legacy_save_token"
}
