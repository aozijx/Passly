package com.aozijx.passly.domain.settings.port

import com.aozijx.passly.domain.settings.model.AppSettingsSnapshot
import com.aozijx.passly.domain.settings.model.SettingsCommand
import kotlinx.coroutines.flow.Flow

/**
 * 统一的设置仓库。
 *
 * 合并了原本的 PortableRepository、DeviceRepository、RuntimeRepository。
 * 对外暴露单一 [settings] Flow，通过 [update] 接收命令式变更。
 * 同时实现 [IdleTimeoutSettings] 供 Session 层直接监听安全相关设置。
 */
interface AppSettingsRepository : IdleTimeoutSettings {
    val settings: Flow<AppSettingsSnapshot>
    suspend fun update(command: SettingsCommand)
}
