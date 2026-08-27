package com.aozijx.passly.app.autofill

import android.content.Context
import android.content.Intent
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchTarget
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchExtras
import com.aozijx.passly.presentation.feature.autofill.credential.CredentialResponseActivity
import com.aozijx.passly.presentation.feature.autofill.legacy.AutofillFillActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAutofillLaunchTarget @Inject constructor() : AutofillLaunchTarget {
    override fun legacyFillIntent(context: Context, requestId: String): Intent =
        Intent(context, AutofillFillActivity::class.java)
            .setPackage(context.packageName)
            .putExtra(AutofillLaunchExtras.REQUEST_ID, requestId)

    override fun credentialResponseIntent(context: Context, requestId: String): Intent =
        Intent(context, CredentialResponseActivity::class.java)
            .setPackage(context.packageName)
            .putExtra(AutofillLaunchExtras.REQUEST_ID, requestId)
}
