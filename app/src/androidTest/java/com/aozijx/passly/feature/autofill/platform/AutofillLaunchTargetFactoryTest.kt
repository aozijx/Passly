package com.aozijx.passly.feature.autofill.platform

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.aozijx.passly.app.autofill.AndroidAutofillLaunchTarget
import com.aozijx.passly.feature.autofill.credential.service.CredentialPendingIntentFactory
import com.aozijx.passly.feature.autofill.legacy.AutofillPendingIntentFactory
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure
import com.aozijx.passly.presentation.feature.autofill.credential.CredentialResponseActivity
import com.aozijx.passly.presentation.feature.autofill.legacy.AutofillFillActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutofillLaunchTargetFactoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun factoriesForwardStableRequestIdsToLaunchTarget() {
        val target = RecordingLaunchTarget(context)

        AutofillPendingIntentFactory(target).createBaseIntent(
            context = context,
            parsed = ParsedStructure(packageName = "com.example"),
            uiMode = com.aozijx.passly.domain.settings.model.AutofillPresentation.SYSTEM_INLINE,
        )
        CredentialPendingIntentFactory(target).createPasswordGetPendingIntent(context, "entry-1")

        assertEquals(
            listOf("autofill-base:com.example", "password-get:entry-1"),
            target.requestIds,
        )
    }

    @Test
    fun androidTargetUsesExplicitActivitiesInCurrentPackage() {
        val target = AndroidAutofillLaunchTarget()

        val legacy = target.legacyFillIntent(context, "legacy")
        val credential = target.credentialResponseIntent(context, "credential")

        assertEquals(context.packageName, legacy.`package`)
        assertEquals(AutofillFillActivity::class.java.name, legacy.component?.className)
        assertEquals(context.packageName, credential.`package`)
        assertEquals(CredentialResponseActivity::class.java.name, credential.component?.className)
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun credentialPendingIntentIdentityAndMutabilityRemainStable() {
        val factory = CredentialPendingIntentFactory(AndroidAutofillLaunchTarget())

        val first = factory.createPasswordGetPendingIntent(context, "entry-1")
        val same = factory.createPasswordGetPendingIntent(context, "entry-1")
        val other = factory.createPasswordGetPendingIntent(context, "entry-2")

        assertEquals(first, same)
        assertNotEquals(first, other)
        assertFalse(first.isImmutable)
    }

    private class RecordingLaunchTarget(private val context: Context) : AutofillLaunchTarget {
        val requestIds = mutableListOf<String>()

        override fun legacyFillIntent(context: Context, requestId: String): Intent {
            requestIds += requestId
            return Intent().setPackage(this.context.packageName)
        }

        override fun credentialResponseIntent(context: Context, requestId: String): Intent {
            requestIds += requestId
            return Intent().setPackage(this.context.packageName)
        }
    }
}
