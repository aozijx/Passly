package com.aozijx.passly.app.message.system

/** Android notification APIs used by [AndroidSystemNotificationGateway]. */
interface SystemNotificationPlatform {
    fun areNotificationsEnabled(): Boolean

    fun channelImportance(channelId: String): Int?

    fun createChannel(
        channelId: String,
        nameResourceId: Int,
        descriptionResourceId: Int,
        importance: Int,
    )

    fun notify(
        channelId: String,
        notificationId: Int,
        title: String,
        text: String,
        smallIconResourceId: Int,
    )
}
