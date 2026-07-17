package com.aozijx.passly.data.repository.settings

import android.content.Context
import com.aozijx.passly.data.local.datastore.appSettingsDataStore
import com.aozijx.passly.domain.repository.settings.RuntimeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuntimeRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : RuntimeRepository {

    private val dataStore = context.applicationContext.appSettingsDataStore
    override val userConfigExtras: Flow<Map<String, String>> =
        dataStore.data.map { it.runtimeExtraMap }

    override suspend fun setUserConfigExtra(key: String, value: String) {
        require(key.isNotBlank()) { "Runtime setting key must not be blank." }
        dataStore.updateData {
            it.toBuilder().putRuntimeExtra(key, value).build()
        }
    }

    override suspend fun getUserConfigExtra(key: String): String? {
        return dataStore.data.first().runtimeExtraMap[key]
    }
}
