package com.aozijx.passly.app.autofill

import android.content.Context
import android.content.Intent
import com.aozijx.passly.app.autofill.presentation.AutofillAuthActivity
import com.aozijx.passly.feature.autofill.platform.AutofillHostLaunchContract
import com.aozijx.passly.feature.autofill.platform.AutofillLaunchTarget
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AndroidAutofillLaunchTarget
    @Inject
    constructor() : AutofillLaunchTarget {
        override fun accessibilityFillIntent(context: Context): Intent =
            Intent(context, AutofillAuthActivity::class.java)
                .setPackage(context.packageName)
                .setAction(AutofillHostLaunchContract.ACTION_ACCESSIBILITY_FILL)

        override fun legacyFillIntent(
            context: Context,
            requestId: String,
        ): Intent =
            Intent(context, AutofillAuthActivity::class.java)
                .setPackage(context.packageName)
                .setAction(AutofillHostLaunchContract.ACTION_LEGACY_FILL)
                .putExtra(AutofillHostLaunchContract.REQUEST_ID, requestId)

        override fun credentialResponseIntent(
            context: Context,
            requestId: String,
        ): Intent =
            Intent(context, AutofillAuthActivity::class.java)
                .setPackage(context.packageName)
                .putExtra(AutofillHostLaunchContract.REQUEST_ID, requestId)
    }
