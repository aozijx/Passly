package com.aozijx.passly.domain.notice.port

import com.aozijx.passly.domain.notice.model.AppMessageSettings

/**
 * 路由时一次性读取的设置快照。
 * 保证同一条 [AppNotice] 在整个路由判断中使用一致的上下文。
 */
data class NoticeRoutingContext(
    val settings: AppMessageSettings,
    val settingsVersion: Long,
    val appVisibility: AppVisibility,
    val systemNotificationState: SystemNotificationState
)

enum class AppVisibility { FOREGROUND, BACKGROUND }

data class SystemNotificationState(
    val userSettingEnabled: Boolean,
    val runtimePermissionGranted: Boolean,
    val notificationsEnabledBySystem: Boolean,
    val channelEnabled: Boolean
) {
    val available: Boolean
        get() = userSettingEnabled &&
            runtimePermissionGranted &&
            notificationsEnabledBySystem &&
            channelEnabled
}

data class VersionedMessageSettings(
    val version: Long,
    val value: AppMessageSettings
)

fun interface MessageSettingsSnapshotProvider {
    fun current(): VersionedMessageSettings
}

fun interface AppVisibilityProvider {
    fun current(): AppVisibility
}

fun interface SystemNotificationStateProvider {
    fun current(): SystemNotificationState
}
