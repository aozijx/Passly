package com.aozijx.passly.app.autofill

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.app.autofill.presentation.AutofillAuthActivity
import com.aozijx.passly.feature.autofill.platform.AutofillHostLaunchContract
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        assertEquals("legacy", legacy.getStringExtra(AutofillHostLaunchContract.REQUEST_ID))
        assertEquals(context.packageName, credential.`package`)
        assertEquals(AutofillAuthActivity::class.java.name, credential.component?.className)
        assertEquals("credential", credential.getStringExtra(AutofillHostLaunchContract.REQUEST_ID))
        assertEquals(context.packageName, accessibility.`package`)
        assertEquals(AutofillAuthActivity::class.java.name, accessibility.component?.className)
        assertEquals(AutofillHostLaunchContract.ACTION_ACCESSIBILITY_FILL, accessibility.action)
    }

    @Test
    fun accessibilityHostCreatesOnlyItsModeViewModel() {
        val intent =
            Intent(context, AutofillAuthActivity::class.java)
                .setAction(AutofillHostLaunchContract.ACTION_ACCESSIBILITY_FILL)
        val viewModelKeys = AtomicReference<Set<Any?>?>(null)
        val callbacks =
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityPaused(activity: Activity) {
                    if (activity is AutofillAuthActivity) {
                        viewModelKeys.set(activity.viewModelStore.keys().toSet())
                    }
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityResumed(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            }

        val application = context.applicationContext as Application
        application.registerActivityLifecycleCallbacks(callbacks)
        try {
            ActivityScenario.launch<AutofillAuthActivity>(intent).use {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            }
        } finally {
            application.unregisterActivityLifecycleCallbacks(callbacks)
        }

        val keys = requireNotNull(viewModelKeys.get())
        assertTrue(keys.any { it.toString().contains(ACCESSIBILITY_VIEW_MODEL) })
        assertFalse(keys.any { it.toString().contains(LEGACY_VIEW_MODEL) })
        assertFalse(keys.any { it.toString().contains(CREDENTIAL_VIEW_MODEL) })
    }

    private companion object {
        const val ACCESSIBILITY_VIEW_MODEL = "com.aozijx.passly.feature.autofill.presentation.accessibility.AccessibilityFillViewModel"
        const val LEGACY_VIEW_MODEL = "com.aozijx.passly.feature.autofill.presentation.legacy.AutofillFillViewModel"
        const val CREDENTIAL_VIEW_MODEL = "com.aozijx.passly.feature.autofill.presentation.credential.CredentialResponseViewModel"
    }
}
