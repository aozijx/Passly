package com.aozijx.passly.data.repository.autofill

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.core.net.toUri
import com.aozijx.passly.data.repository.autofill.AutofillStatusRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AutofillStatusRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AutofillStatusRepository {

    override fun isAutofillServiceEnabled(): Boolean {
        val currentService = Settings.Secure.getString(context.contentResolver, "autofill_service")
        val selected = currentService?.let { ComponentName.unflattenFromString(it) } ?: return false

        // 校验包名是否匹配。
        // 注意：根据具体的 Service 类名可能需要更精确的校验。
        return selected.packageName == context.packageName
    }

    override fun observeAutofillStatus(): Flow<Boolean> = callbackFlow {
        val observer = object : android.database.ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(isAutofillServiceEnabled())
            }
        }

        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor("autofill_service"),
            false,
            observer
        )

        trySend(isAutofillServiceEnabled())

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.onStart { emit(isAutofillServiceEnabled()) }

    override fun isAutofillSupported(): Boolean {
        val manager = context.getSystemService(AutofillManager::class.java)
        return manager != null && manager.isAutofillSupported
    }

    override fun openAutofillSettings() {
        val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
