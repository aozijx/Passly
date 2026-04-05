package com.aozijx.passly.domain.repository

import com.aozijx.passly.domain.model.UserConfig
import kotlinx.coroutines.flow.Flow

interface UserConfigRepository {
    val userConfig: Flow<UserConfig>
    suspend fun save(config: UserConfig)
    suspend fun update(transform: (UserConfig) -> UserConfig)
    suspend fun reset()
}
