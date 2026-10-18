package com.aozijx.passly.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.aozijx.passly.data.preferences.proto.AppPreferences

val Context.appPreferencesDataStore: DataStore<AppPreferences> by dataStore(
    fileName = "app_preferences.pb",
    serializer = AppPreferencesSerializer,
)
