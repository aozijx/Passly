package com.aozijx.passly.app.autofill

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.app.autofill.presentation.AutofillAuthActivity
import com.aozijx.passly.feature.autofill.platform.AutofillHostLaunchContract
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidAutofillLaunchTargetTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun targetRoutesEveryAutofillKindToOnePrivateActivityWithStableKinds() {
        val target = AndroidAutofillLaunchTarget()

        val legacy = target.legacyFillIntent(context, "legacy")
        val credential = target.credentialResponseIntent(context, "credential")
        val accessibility = target.accessibilityFillIntent(context)

        assertEquals(context.packageName, legacy.`package`)
        assertEquals(AutofillAuthActivity::class.java.name, legacy.component?.className)
        assertEquals(AutofillHostLaunchContract.ACTION_LEGACY_FILL, legacy.action)
        assertEquals(context.packageName, credential.`package`)
        assertEquals(AutofillAuthActivity::class.java.name, credential.component?.className)
        assertEquals(context.packageName, accessibility.`package`)
        assertEquals(AutofillAuthActivity::class.java.name, accessibility.component?.className)
        assertEquals(AutofillHostLaunchContract.ACTION_ACCESSIBILITY_FILL, accessibility.action)
    }
}
