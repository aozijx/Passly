package com.aozijx.passly.app.message.contract

import com.aozijx.passly.domain.settings.model.MessageSettings

/**
 * 路由时一次性读取的设置快照。
 * 保证同一条 [AppNotice] 在整个路由判断中使用一致的上下文。
 */
data class NoticeRoutingContext(
    val settings: MessageSettings,
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

sealed interface MessageSettingsSnapshot {
    data object Loading : MessageSettingsSnapshot

    data class Ready(
        val version: Long,
        val value: MessageSettings,
    ) : MessageSettingsSnapshot
}

fun interface MessageSettingsSnapshotProvider {
    fun current(): MessageSettingsSnapshot
}

fun interface AppVisibilityProvider {
    fun current(): AppVisibility
}

fun interface SystemNotificationStateProvider {
    fun current(): SystemNotificationState
}
