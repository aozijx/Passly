package com.aozijx.passly.feature.autofill.attack

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class AutofillOriginAttackInstrumentedTest {
    @Test
    fun unassociatedTestPackageCanSupplyAnotherWebDomainInTheSystemStructure() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val testContext = instrumentation.context
        val preferences = testContext.getSharedPreferences(CAPTURE_PREFERENCES, Context.MODE_PRIVATE)
        val previousAutofillService = shell("settings get secure autofill_service").trim()
        val serviceName = "${testContext.packageName}/${CaptureAutofillService::class.java.name}"

        try {
            preferences.edit().clear().commit()
            shell("settings put secure autofill_service $serviceName")
            testContext.startActivity(
                Intent(testContext, AutofillOriginAttackActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )

            val deadline = SystemClock.uptimeMillis() + 10_000
            while (!preferences.getBoolean(CAPTURE_RECEIVED, false) && SystemClock.uptimeMillis() < deadline) {
                SystemClock.sleep(25)
            }

            assertTrue("system never called the capture service", preferences.getBoolean(CAPTURE_RECEIVED, false))
            assertEquals(testContext.packageName, preferences.getString(CAPTURE_PACKAGE, null))
            val applicationInfo = testContext.packageManager.getApplicationInfo(testContext.packageName, PackageManager.GET_META_DATA)
            assertFalse("the test app manifest must not claim a Digital Asset Links association", applicationInfo.metaData?.containsKey("asset_statements") == true)
            assertEquals(CLAIMED_WEB_DOMAIN, preferences.getString(CAPTURE_DOMAIN, null))
            assertFalse(preferences.getBoolean(CAPTURE_AMBIGUOUS, true))
            assertTrue("the custom structure must contain both editable test fields", preferences.getInt(CAPTURE_FIELD_COUNT, 0) >= 2)
        } finally {
            testContext.sendBroadcast(Intent(FINISH_ATTACK_ACTIVITY).setPackage(testContext.packageName))
            if (previousAutofillService == "null") {
                shell("settings delete secure autofill_service")
            } else {
                shell("settings put secure autofill_service \"$previousAutofillService\"")
            }
            assertEquals(
                "secure autofill_service setting was not restored",
                previousAutofillService,
                shell("settings get secure autofill_service"),
            )
        }
    }

    private fun shell(command: String): String =
        instrumentationOutput(command).trim()

    private fun instrumentationOutput(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
    }
}
