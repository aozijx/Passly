package com.aozijx.passly.domain.repository.settings

import kotlinx.coroutines.flow.Flow

interface RuntimeRepository {
    val userConfigExtras: Flow<Map<String, String>>
    suspend fun setUserConfigExtra(key: String, value: String)
    suspend fun getUserConfigExtra(key: String): String?
}