package com.aozijx.passly.features.vault.internal

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.core.net.toUri
import com.aozijx.passly.core.logging.Logcat

internal class VaultAutofillSupport {

    fun isAutofillEnabled(context: Context): Boolean {
        val afm = context.getSystemService(AutofillManager::class.java)
        val currentService = Settings.Secure.getString(context.contentResolver, "autofill_service")
        val isOurServiceSelected = currentService != null && currentService.contains(context.packageName)
        val isEnabledByApi = afm != null && afm.isEnabled && afm.hasEnabledAutofillServices()
        return isOurServiceSelected || isEnabledByApi
    }

    fun openAutofillSettings(context: Context): Boolean {
        val standardIntent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
            data = "package:${context.packageName}".toUri()
        }

        var started = tryStartActivity(context, standardIntent)

        if (!started) {
            started = tryStartActivity(context, Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        }

        if (!started) {
            started = tryStartActivity(context, Intent(Settings.ACTION_SETTINGS))
        }

        return started
    }

    private fun tryStartActivity(context: Context, intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Logcat.e("VaultAutofillSupport", "Failed to start activity: ${intent.action}", e)
            false
        }
    }
}
