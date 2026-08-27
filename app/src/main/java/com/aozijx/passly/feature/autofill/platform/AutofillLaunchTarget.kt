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
}
