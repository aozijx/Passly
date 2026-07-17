package com.aozijx.passly.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.aozijx.passly.data.local.datastore.settings.AppSettings

/**
 * Proto DataStore that replaces the old Preferences-based vault_settings store.
 */
val Context.appSettingsDataStore: DataStore<AppSettings> by dataStore(
    fileName = "app_settings.pb",
    serializer = AppSettingsSerializer
)
