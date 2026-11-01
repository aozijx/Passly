package com.aozijx.passly.app.autofill

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.aozijx.passly.feature.autofill.platform.AutofillServiceStatusSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AndroidAutofillServiceStatusSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AutofillServiceStatusSource {
    private fun isServiceEnabled(): Boolean {
        val currentService = Settings.Secure.getString(context.contentResolver, "autofill_service")
        val selected = currentService?.let(ComponentName::unflattenFromString) ?: return false
        return selected.packageName == context.packageName
    }

    override fun observeServiceEnabled(): Flow<Boolean> = callbackFlow {
        var registered = false
        val sourceFailure = AtomicReference<Throwable?>()
        fun publishStatus() {
            try {
                trySend(isServiceEnabled())
            } catch (exception: Exception) {
                sourceFailure.set(exception)
                close(exception)
            } catch (error: Error) {
                sourceFailure.set(error)
                close(error)
                throw error
            }
        }
        val observer = object : android.database.ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                publishStatus()
            }
        }
        try {
            context.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor("autofill_service"),
                false,
                observer,
            )
            registered = true
            // Keep the observer registered before taking the snapshot so no setting change is lost.
            publishStatus()
            awaitClose()
        } catch (error: Throwable) {
            sourceFailure.set(error)
            throw error
        } finally {
            if (registered) {
                try {
                    context.contentResolver.unregisterContentObserver(observer)
                } catch (cleanupFailure: Throwable) {
                    val failure = sourceFailure.get()
                    if (failure == null) throw cleanupFailure
                    if (cleanupFailure !== failure) failure.addSuppressed(cleanupFailure)
                }
            }
        }
    }.onStart { emit(isServiceEnabled()) }
}
