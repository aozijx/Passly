package com.aozijx.passly.app.autofill

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import com.aozijx.passly.feature.autofill.platform.AutofillPlatformGateway
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AndroidAutofillPlatformGateway @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AutofillPlatformGateway {
    private fun isServiceEnabled(): Boolean {
        val currentService = Settings.Secure.getString(context.contentResolver, "autofill_service")
        val selected = currentService?.let(ComponentName::unflattenFromString) ?: return false
        return selected.packageName == context.packageName
    }

    override fun observeServiceEnabled(): Flow<Boolean> = callbackFlow {
        val observer = object : android.database.ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(isServiceEnabled())
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor("autofill_service"),
            false,
            observer,
        )
        trySend(isServiceEnabled())
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.onStart { emit(isServiceEnabled()) }

    override fun openSystemSettings() {
        val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
