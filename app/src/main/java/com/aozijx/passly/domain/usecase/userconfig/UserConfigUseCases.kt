package com.aozijx.passly.domain.usecase.userconfig

import com.aozijx.passly.domain.model.UserConfig
import com.aozijx.passly.domain.repository.userconfig.UserConfigRepository
import kotlinx.coroutines.flow.Flow

class UserConfigUseCases(private val repository: UserConfigRepository) {
    val userConfig: Flow<UserConfig> = repository.userConfig

    suspend fun save(config: UserConfig) = repository.save(config)

    suspend fun update(transform: (UserConfig) -> UserConfig) = repository.update(transform)

    suspend fun reset() = repository.reset()
}
