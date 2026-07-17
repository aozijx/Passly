package com.aozijx.passly.domain.usecase.settings

import com.aozijx.passly.domain.repository.settings.RuntimeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 运行时设置用例：键值对形式的运行时配置附加项。
 */
@Singleton
class RuntimeSettingsUseCases @Inject constructor(
    private val repository: RuntimeRepository
) {
    val userConfigExtras: Flow<Map<String, String>> = repository.userConfigExtras

    suspend fun setUserConfigExtra(key: String, value: String) =
        repository.setUserConfigExtra(key, value)

    suspend fun getUserConfigExtra(key: String): String? =
        repository.getUserConfigExtra(key)
}