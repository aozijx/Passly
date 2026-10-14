package com.aozijx.passly.feature.autofill.platform

import android.content.Context
import android.content.Intent

interface AutofillLaunchTarget {
    fun legacyFillIntent(context: Context, requestId: String): Intent
    fun credentialResponseIntent(context: Context, requestId: String): Intent
}

object AutofillLaunchExtras {
    const val REQUEST_ID = "autofill_request_id"
    const val RETURN_DATASET = "autofill_return_dataset"
    const val INTERACTION_ID = "autofill_interaction_id"
    const val ENTRY_ID = "autofill_entry_id"
    const val UI_MODE = "autofill_ui_mode"
    const val PACKAGE_NAME = "autofill_package_name"
    const val WEB_DOMAIN = "autofill_web_domain"
    const val EDITABLE_IDS = "autofill_editable_ids"
    const val USERNAME_IDS = "autofill_username_ids"
    const val PASSWORD_IDS = "autofill_password_ids"
    const val OTP_IDS = "autofill_otp_ids"
}
