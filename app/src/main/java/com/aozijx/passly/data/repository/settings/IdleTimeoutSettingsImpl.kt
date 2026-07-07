package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.core.auth.session.IdleTimeoutSettings
import com.aozijx.passly.domain.repository.settings.SecuritySettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IdleTimeoutSettings 的实现类
 * 从 SecuritySettingsRepository 获取后台锁定设置
 */
@Singleton
class IdleTimeoutSettingsImpl @Inject constructor(
    private val securitySettingsRepository: SecuritySettingsRepository
) : IdleTimeoutSettings {
    override val lockOnBackground: Flow<Boolean> = securitySettingsRepository.isLockOnBackground
}