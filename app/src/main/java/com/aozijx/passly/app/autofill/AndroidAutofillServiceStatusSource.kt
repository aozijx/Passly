package com.aozijx.passly.app.autofill

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
internal class AndroidAutofillServiceStatusSource
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : AutofillServiceStatusSource {
        private fun isServiceEnabled(): Boolean {
            val currentService = Settings.Secure.getString(context.contentResolver, "autofill_service")
            val selected = currentService?.let(ComponentName::unflattenFromString) ?: return false
            return selected.packageName == context.packageName
        }

        override fun observeServiceEnabled(): Flow<Boolean> =
            observeBooleanStatus(
                read = ::isServiceEnabled,
                registerObserver = { onChange ->
                    val observer =
                        object : android.database.ContentObserver(null) {
                            override fun onChange(selfChange: Boolean) {
                                onChange()
                            }
                        }
                    context.contentResolver.registerContentObserver(
                        Settings.Secure.getUriFor("autofill_service"),
                        false,
                        observer,
                    )
                    val unregister: () -> Unit = {
                        context.contentResolver.unregisterContentObserver(observer)
                    }
                    unregister
                },
            )
    }
