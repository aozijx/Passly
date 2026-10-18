package com.aozijx.passly.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.aozijx.passly.data.preferences.proto.SecurityPreferences

val Context.securityPreferencesDataStore: DataStore<SecurityPreferences> by dataStore(
    fileName = "security_preferences.pb",
    serializer = SecurityPreferencesSerializer,
)
