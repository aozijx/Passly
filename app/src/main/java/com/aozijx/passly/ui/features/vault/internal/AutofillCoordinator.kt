package com.aozijx.passly.ui.features.vault.internal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import com.aozijx.passly.R
import com.aozijx.passly.core.log.Logcat

internal class AutofillCoordinator {
    var isEnabled by mutableStateOf(false)
        private set

    private companion object {
        const val AUTOFILL_SERVICE_CLASS =
            "com.aozijx.passly.service.autofill.LegacyAutofillService"
    }

    fun refreshStatus(context: Context) {
        val currentService = Settings.Secure.getString(context.contentResolver, "autofill_service")
        val selected = currentService?.let { ComponentName.unflattenFromString(it) }
        isEnabled =
            selected != null && selected.packageName == context.packageName && selected.className == AUTOFILL_SERVICE_CLASS
    }

    fun requestEnable(context: Context) {
        if (!openSettings(context)) {
            Toast.makeText(context, R.string.vault_toast_enable_autofill_manual, Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun openSettings(context: Context): Boolean {
        val standardIntent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
            data = "package:${context.packageName}".toUri()
        }
        return tryStartActivity(context, standardIntent) || tryStartActivity(
            context,
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        ) || tryStartActivity(context, Intent(Settings.ACTION_SETTINGS))
    }

    private fun tryStartActivity(context: Context, intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Logcat.e("AutofillCoordinator", "Failed to start activity: ${intent.action}", e)
            false
        }
    }
}